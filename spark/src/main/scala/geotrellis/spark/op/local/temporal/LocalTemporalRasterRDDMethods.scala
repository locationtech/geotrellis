package geotrellis.spark.op.local.temporal

import geotrellis.raster._
import geotrellis.raster.op.local._

import geotrellis.spark._
import geotrellis.spark.op._

import org.joda.time._

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
    end: DateTime): RasterRDD[K] = ???

  private def aggregateWithTemporalWindow(
    windowSize: Int,
    unit: Int,
    base: DateTime,
    end: DateTime)(
    reduceOp: Seq[Tile] => Tile
  ): RasterRDD[K] = {
    val sc = rasterRDD.sparkContext

    @tailrec
    def recurse(index: Int = 0, tail: RDD[(K, Tile)] = sc.emptyRDD): RasterRDD[K] =
      if (index == windowSize) new RasterRDD[K](tail, rasterRDD.metaData)
      else {
        val reducedRDD = rasterRDD
          .filter { case(key, tile) =>
            val TemporalKey(time) = key.temporalComponent
            val diff = getDifferenceByUnit(unit, base, time)
            val endDiff = getDifferenceByUnit(unit, time, end)
            diff >= index && endDiff >= 0
        }
          .map { case (key, tile) =>
            val SpatialKey(col, row) = key.spatialComponent
            val TemporalKey(time) = key.temporalComponent

            val diff = getDifferenceByUnit(unit, base, time)

            val timeDelimiter = (diff - index) / windowSize
            val newKey = (timeDelimiter, col, row)

            (newKey, (key, tile, diff % windowSize == index))
        }
          .groupByKey
          .map { case(tempKey, iter) =>
            val seq = iter.toSeq
            val key = seq.filter(_._3).head._1
            val tiles = seq.map(_._2)
            (key, reduceOp(tiles))
        }

        recurse(index + 1, tail ++ reducedRDD)
      }

    recurse()
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

  private def minReduceOp(tiles: Seq[Tile]) = tiles.localMin

  private def maxReduceOp(tiles: Seq[Tile]) = tiles.localMax

  private def meanReduceOp(tiles: Seq[Tile]) = tiles.localMean

}
