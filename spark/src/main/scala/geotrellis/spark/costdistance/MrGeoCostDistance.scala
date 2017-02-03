package geotrellis.spark.costdistance

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.raster.costdistance.CostDistance

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.util.AccumulatorV2

import scala.collection.mutable


/**
  * This Spark-enabled implementation of the standard cost-distance
  * algorithm [1] is "heavily inspired" by the MrGeo implementation
  * [2] but does not share any code with it.
  *
  * 1. Tomlin, Dana.
  *    "Propagating radial waves of travel cost in a grid."
  *    International Journal of Geographical Information Science 24.9 (2010): 1391-1413.
  *
  * 2. https://github.com/ngageoint/mrgeo/blob/0c6ed4a7e66bb0923ec5c570b102862aee9e885e/mrgeo-mapalgebra/mrgeo-mapalgebra-costdistance/src/main/scala/org/mrgeo/mapalgebra/CostDistanceMapOp.scala
  */
object MrGeoCostDistance {

  type CostList = List[CostDistance.Cost]
  type KeyListPair = (SpatialKey, CostList)
  type PixelMap = mutable.Map[SpatialKey, CostList]

  /**
    * An accumulator to hold lists of edge changes.
    */
  class PixelMapAccumulator extends AccumulatorV2[KeyListPair, PixelMap] {
    private var map: PixelMap = mutable.Map.empty

    def copy: PixelMapAccumulator = {
      val other = new PixelMapAccumulator
      other.merge(this)
      other
    }
    def add(kv: (SpatialKey, CostList)): Unit = {
      val key = kv._1
      val list = kv._2

      if (!map.contains(key)) map.put(key, list)
      else map.put(key, map.get(key).get ++ list)
    }
    def isZero: Boolean = map.isEmpty
    def merge(other: AccumulatorV2[(SpatialKey, CostList), PixelMap]): Unit = map ++= other.value
    def reset: Unit = map = mutable.Map.empty
    def value: PixelMap = map
  }

  /**
    * Perform the cost-distance computation.
    */
  def apply[K: (? => SpatialKey), V: (? => Tile)](
    friction: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    points: List[Point],
    maxCost: Double = Double.PositiveInfinity
  )(implicit sc: SparkContext): RDD[(K, Tile)] = {

    val accumulator = new PixelMapAccumulator
    sc.register(accumulator, "Pixel Map Accumulator")

    val mt = friction.metadata.mapTransform

    // Load the accumulator with the starting values
    friction.foreach({ case (k, v) =>
      val key = implicitly[SpatialKey](k)
      val tile = implicitly[Tile](v)
      val cols = tile.cols
      val rows = tile.rows
      val extent = mt(key)
      val rasterExtent = RasterExtent(extent, cols, rows)
      val costs: List[CostDistance.Cost] = points
        .filter({ point => extent.contains(point) })
        .map({ point =>
          val col = rasterExtent.mapXToGrid(point.x)
          val row = rasterExtent.mapYToGrid(point.y)
          val friction = tile.getDouble(col, row)
          (col, row, friction, 0.0)
        })

      accumulator.add((key, costs))
    })

    // Create RDD of initial (empty) cost tiles
    var costs: RDD[(K, V, DoubleArrayTile)] = friction.map({ case (k, v) =>
      val tile = implicitly[Tile](v)
      val cols = tile.cols
      val rows = tile.rows

      (k, v, CostDistance.generateEmptyCostTile(cols, rows))
    })

    // XXX Should be shared variable
    val changes = accumulator.value

    // do {
    costs = costs.map({ case (k, v, cost) =>
      val key = implicitly[SpatialKey](k)
      val friction = implicitly[Tile](v)
      val cols = friction.cols
      val rows = friction.rows
      val q: CostDistance.Q = {
        val q = CostDistance.generateEmptyQueue(cols, rows)

        changes
          .getOrElse(key, List.empty[CostDistance.Cost])
          .foreach({ (entry: CostDistance.Cost) => q.add(entry) })
        q
      }

      (k, v, CostDistance.compute(friction, cost, maxCost, q, CostDistance.nop))
      // (k, v, CostDistance.compute(CostDistance.generateEmptyCostTile(cols, rows), cost, maxCost, q, CostDistance.nop))
    })

    accumulator.reset
    // } while (changes.size > 0)

    costs.map({ case (k, _, cost) => (k, cost) })
  }
}
