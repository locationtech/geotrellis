package geotrellis.spark.op.local.temporal

import geotrellis.raster._
import geotrellis.raster.op.local._

import geotrellis.spark._
import geotrellis.spark.op._

import org.joda.time.DateTime

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import annotation.tailrec

trait LocalTemporalRasterRDDMethods[K] extends RasterRDDMethods[K] with Serializable {

  import TemporalWindowHelper._

  implicit val _sc: SpatialComponent[K]

  implicit val _tc: TemporalComponent[K]

  private def reduceOp(tileReducer: (Tile, Tile) => Tile)(
    reduce: (K, Tile, Boolean), next: (K, Tile, Boolean)) = {
    val (k1, t1, firstIsCorrectKey) = reduce
    val (k2, t2, secondIsCorrectKey) = next

    val tile = tileReducer(t1, t2)

    val (key, isCorrectKey) =
      if (firstIsCorrectKey) (k1, true)
      else if (secondIsCorrectKey) (k2, true)
      else (k1, false)

    (key, tile, isCorrectKey)
  }

  private def minReduceOp = reduceOp(minTileReduceOp) _

  private def minTileReduceOp(t1: Tile, t2: Tile) = t1.localMin(t2)

  private def maxReduceOp = reduceOp(maxTileReduceOp) _

  private def maxTileReduceOp(t1: Tile, t2: Tile) = t1.localMax(t2)

  def temporalMin(periodStep: Int, unit: Int, start: DateTime): RasterRDD[K] =
    aggregateWithTemporalWindow(periodStep, unit, start)(minReduceOp)

  def temporalMax(periodStep: Int, unit: Int, start: DateTime): RasterRDD[K] =
    aggregateWithTemporalWindow(periodStep, unit, start)(maxReduceOp)

  private def aggregateWithTemporalWindow(
    periodStep: Int,
    unit: Int,
    start: DateTime)(
    reduceOp: ((K, Tile, Boolean), (K, Tile, Boolean)) => (K, Tile, Boolean)
  ): RasterRDD[K] = {
    val sc = rasterRDD.sparkContext

    @tailrec
    def recurse(index: Int = 0, tail: RDD[(K, Tile)] = sc.emptyRDD): RasterRDD[K] =
      if (index == periodStep) new RasterRDD[K](tail, rasterRDD.metaData)
      else {
        val reducedRDD = rasterRDD.map { case (key, tile) =>
          val SpatialKey(col, row) = key.spatialComponent
          val TemporalKey(time) = key.temporalComponent

          val year = time.getYear
          val diff = start.getYear - year

          val timeDelimiter = diff % periodStep + index
          val newKey = (timeDelimiter, col, row)

          (newKey, (key, tile, timeDelimiter == year))
        }.reduceByKey(reduceOp)
          .map { case(_, (key, tile, _)) => (key, tile) }

        recurse(index + 1, tail ++ reducedRDD)
      }

    recurse()
  }

}
