package geotrellis.spark.costdistance

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.costdistance.CostDistance
import geotrellis.spark._
import geotrellis.vector._

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
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
object IterativeCostDistance {

  type CostList = List[CostDistance.Cost]
  type KeyListPair = (SpatialKey, CostList)
  type Changes = mutable.Map[SpatialKey, CostList]

  val logger = Logger.getLogger(IterativeCostDistance.getClass)

  /**
    * An accumulator to hold lists of edge changes.
    */
  class ChangesAccumulator extends AccumulatorV2[KeyListPair, Changes] {
    private var map: Changes = mutable.Map.empty

    def copy: ChangesAccumulator = {
      val other = new ChangesAccumulator
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
    def merge(other: AccumulatorV2[(SpatialKey, CostList), Changes]): Unit = map ++= other.value
    def reset: Unit = map.clear
    def value: Changes = map
  }

  /**
    * Perform the cost-distance computation.
    *
    * @param  friction  The friction layer; pixels are in units of "seconds per meter"
    * @param  points    The starting locations from-which to compute the cost of traveling
    * @param  maxCost   The maximum cost before pruning a path (in units of "seconds")
    */
  def apply[K: (? => SpatialKey), V: (? => Tile)](
    friction: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    points: List[Point],
    maxCost: Double = Double.PositiveInfinity
  )(implicit sc: SparkContext): RDD[(K, Tile)] = {

    val md = friction.metadata

    val mt = md.mapTransform

    val resolution = {
      val kv = friction.first
      val key = implicitly[SpatialKey](kv._1)
      val tile = implicitly[Tile](kv._2)
      val extent = mt(key).reproject(md.crs, LatLng)
      val degrees = extent.xmax - extent.xmin
      val meters = degrees * (6378137 * 2.0 * math.Pi) / 360.0
      val pixels = tile.cols
      math.abs(meters / pixels)
    }
    logger.debug(s"Computed resolution: $resolution meters/pixel")

    val bounds = friction.metadata.bounds.asInstanceOf[KeyBounds[K]]
    val minKey = implicitly[SpatialKey](bounds.minKey)
    val maxKey = implicitly[SpatialKey](bounds.maxKey)

    val accumulator = new ChangesAccumulator
    sc.register(accumulator, "Changes")

    // Create RDD of initial (empty) cost tiles and load the
    // accumulator with the starting values.
    var costs: RDD[(K, V, DoubleArrayTile)] = friction.map({ case (k, v) =>
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

      if (costs.length > 0)
        accumulator.add((key, costs))

      (k, v, CostDistance.generateEmptyCostTile(cols, rows))
    })

    // Repeatedly map over the RDD of cost tiles until no more changes
    // occur on the periphery of any tile.
    do {
      val changes = sc.broadcast(accumulator.value.toMap)
      logger.debug(s"At least ${changes.value.size} changed tiles")

      accumulator.reset

      val previous = costs

      costs = previous.map({ case (k, v, oldCostTile) =>
        val key = implicitly[SpatialKey](k)
        val frictionTile = implicitly[Tile](v)
        val keyCol = key._1
        val keyRow = key._2
        val frictionTileCols = frictionTile.cols
        val frictionTileRows = frictionTile.rows
        val localChanges = changes.value.getOrElse(key, List.empty[CostDistance.Cost])

        if (localChanges.length > 0) {
          val q: CostDistance.Q = CostDistance.generateEmptyQueue(frictionTileCols, frictionTileRows)
          localChanges.foreach({ (entry: CostDistance.Cost) => q.add(entry) })

          val buffer: mutable.ArrayBuffer[CostDistance.Cost] = mutable.ArrayBuffer.empty

          val newCostTile = CostDistance.compute(
            frictionTile, oldCostTile,
            maxCost, resolution,
            q, { (entry: CostDistance.Cost) => buffer.append(entry) }
          )

          // Register changes on left periphery
          if (minKey._1 <= keyCol-1) {
            val leftKey = SpatialKey(keyCol-1, keyRow)
            val leftList = buffer
              .filter({ (entry: CostDistance.Cost) => entry._1 == 0 })
              .map({ case (_, row: Int, f: Double, c: Double) =>
                (frictionTileCols, row, f, c) })
              .toList
            if (leftList.size > 0)
              accumulator.add((leftKey, leftList))
          }

          // Register changes on upper periphery
          if (keyRow+1 <= maxKey._2) {
            val upKey = SpatialKey(keyCol, keyRow+1)
            val upList = buffer
              .filter({ (entry: CostDistance.Cost) => entry._2 == frictionTileRows-1 })
              .map({ case (col: Int, _, f: Double, c: Double) => (col, -1, f, c) })
              .toList
            if (upList.size > 0)
              accumulator.add((upKey, upList))
          }

          // Register changes on right periphery
          if (keyCol+1 <= maxKey._1) {
            val rightKey = SpatialKey(keyCol+1, keyRow)
            val rightList = buffer
              .filter({ (entry: CostDistance.Cost) => entry._1 == frictionTileCols-1 })
              .map({ case (_, row: Int, f: Double, c: Double) => (-1, row, f, c) })
              .toList
            if (rightList.size > 0)
              accumulator.add((rightKey, rightList))
          }

          // Register changes on lower periphery
          if (minKey._2 <= keyRow-1) {
            val downKey = SpatialKey(keyCol, keyRow-1)
            val downList = buffer
              .filter({ (entry: CostDistance.Cost) => entry._2 == 0 })
              .map({ case (col: Int, _, f: Double, c: Double) =>
                (col, frictionTileRows, f, c) })
              .toList
            if (downList.size > 0)
              accumulator.add((downKey, downList))
          }

          // XXX It would be slightly more correct to include the four
          // diagonal tiles as well, but there would be at most a one
          // pixel contribution each, so it probably is not worth the
          // expense.

          (k, v, newCostTile)
        }
        else
          (k, v, oldCostTile)
      }).persist(StorageLevel.MEMORY_AND_DISK_SER)

      costs.count
      previous.unpersist()
    } while (accumulator.value.size > 0)

    costs.map({ case (k, _, cost) => (k, cost) })
  }
}
