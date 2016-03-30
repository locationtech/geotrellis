package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import org.apache.spark.Partitioner
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._
import org.joda.time._
import com.github.nscala_time.time.Imports._

import scala.annotation.tailrec
import scala.reflect.ClassTag

object LocalTemporalStatistics {
  import TemporalWindowHelper._

  def temporalMin[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(minReduceOp)

  def temporalMax[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(maxReduceOp)

  def temporalMean[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(meanReduceOp)

  def temporalVariance[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(varianceReduceOp)

  private def aggregateWithTemporalWindow[K: ClassTag: SpatialComponent: TemporalComponent](
    sourceRdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: DateTime,
    end: DateTime,
    partitioner: Option[Partitioner] = None)(
    reduceOp: Traversable[Tile] => Tile
  ): RDD[(K, Tile)] = {
    val rdd =
      sourceRdd
        .map { case (key, tile) =>
          val SpatialKey(col, row) = key.getComponent[SpatialKey]
          val time = key.getComponent[TemporalKey].time
          val startDiff = getDifferenceByUnit(unit, start, time)
          val endDiff = getDifferenceByUnit(unit, time, end)

          val newKey =
            if (startDiff < 0 && endDiff < 0) {
              (-1, col, row)
            }
            else {
              val timeDelimiter = startDiff / windowSize
              (timeDelimiter, col, row)
            }

          (newKey, (key, tile))
        }
        .filter { case ((i, col, row), _) => i >= 0 }

    partitioner
      .fold(rdd.groupByKey())(rdd.groupByKey(_))
      .map { case (_, iter) =>
        val (keys, tiles) = iter.unzip

        val key = keys.min(Ordering.by { key: K => key.getComponent[TemporalKey].time })
        val tile = reduceOp(tiles)

        (key, tile)
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

  private def minReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMin

  private def maxReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMax

  private def meanReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMean

  private def varianceReduceOp(tiles: Traversable[Tile]): Tile = tiles.localVariance

}
