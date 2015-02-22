package geotrellis.spark.op.local.temporal

import geotrellis.raster._
import geotrellis.raster.op.local._

import geotrellis.spark._
import geotrellis.spark.op._

import org.joda.time._
import com.github.nscala_time.time.Imports._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import annotation.tailrec

// TODO: break out stuff to companion object.
trait LocalTemporalRasterRDDMethods[K] extends RasterRDDMethods[K] with Serializable {

  import TemporalWindowHelper._

  implicit val _sc: SpatialComponent[K]

  implicit val _tc: TemporalComponent[K]

  def temporalMin(
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime): RasterRDD[K] =
    aggregateWithTemporalWindow(windowSize, unit, start, end)(minReduceOp)

  def temporalMax(
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime): RasterRDD[K] =
    aggregateWithTemporalWindow(windowSize, unit, start, end)(maxReduceOp)

  def temporalMean(
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime): RasterRDD[K] =
    aggregateWithTemporalWindow(windowSize, unit, start, end)(meanReduceOp)

  def temporalVariance(
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime): RasterRDD[K] =
    aggregateWithTemporalWindow(windowSize, unit, start, end)(varianceReduceOp)

  private def aggregateWithTemporalWindow(
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime)(
    reduceOp: Traversable[Tile] => Tile
  ): RasterRDD[K] = {
    val sc = rasterRDD.sparkContext

    asRasterRDD(rasterRDD.metaData) {
      rasterRDD
        .map { case (key, tile) =>
          val SpatialKey(col, row) = key.spatialComponent
          val TemporalKey(time) = key.temporalComponent
          val startDiff = getDifferenceByUnit(unit, start, time)
          val endDiff = getDifferenceByUnit(unit, time, end)

          val newKey = 
            if(startDiff < 0 && endDiff < 0) {
              (-1, col, row)
            }
            else {
              val timeDelimiter = startDiff / windowSize
              (timeDelimiter, col, row)
            }

          (newKey, (key, tile))
         }
        .filter { case ((i, col, row), _) => i >= 0 }
        .groupByKey
        .map { case (_, iter) =>
          val (keys, tiles) = iter.unzip

          val key = keys.min(Ordering.by { key: K => key.temporalComponent.time })
          val tile = reduceOp(tiles)

          (key, tile)
        }
    }
  }

  private def getDifferenceByUnit(unit: Int, base: DateTime, time: DateTime) =
    unit match {
      case UnitSeconds => Seconds.secondsBetween(base, time).getSeconds
      case UnitMinutes => Minutes.minutesBetween(base, time).getMinutes
      case UnitHours => Hours.hoursBetween(base, time).getHours
      case UnitDays => Days.daysBetween(base, time).getDays
      case UnitWeeks => Weeks.weeksBetween(base, time).getWeeks
      case UnitMonths => Months.monthsBetween(base, time).getMonths
      case UnitYears => Years.yearsBetween(base, time).getYears
      case _ => throw new IllegalStateException(s"Bad unit $unit.")
    }


  // If the raster local operations doesn't have the operation you need as
  // a operation on tile sequences, just create it through a reduce.

  private def minReduceOp(tiles: Traversable[Tile]) = tiles.localMin

  private def maxReduceOp(tiles: Traversable[Tile]) = tiles.localMax

  private def meanReduceOp(tiles: Traversable[Tile]) = tiles.localMean

  private def varianceReduceOp(tiles: Traversable[Tile]) = tiles.localVariance

}
