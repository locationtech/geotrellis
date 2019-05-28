package geotrellis.layers.mapalgebra.local.temporal

import geotrellis.raster.Tile
import geotrellis.tiling.{SpatialComponent, SpatialKey, TemporalComponent, TemporalKey}
import geotrellis.util._

import java.time._
import java.time.temporal.ChronoUnit._
import jp.ne.opt.chronoscala.Imports._

import scala.reflect.ClassTag


object LocalTemporalStatistics {
  import TemporalWindowHelper._

  def temporalMin[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(minReduceOp)

  def temporalMax[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(maxReduceOp)

  def temporalMean[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(meanReduceOp)

  def temporalVariance[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(varianceReduceOp)

  private[geotrellis] def aggregateWithTemporalWindow[K: SpatialComponent: TemporalComponent](
    sourceSeq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime)(reduceOp: Traversable[Tile] => Tile): Seq[(K, Tile)] = {
    val seq =
      sourceSeq
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

    seq.groupBy(_._1)
      .map { case (k, v) => (k, v.map(_._2)) }
      .map { case (_, iter) =>
        val (keys, tiles) = iter.unzip

        val key = keys.min(Ordering.by { key: K => key.getComponent[TemporalKey].time })
        val tile = reduceOp(tiles)

        (key, tile)
      }.toSeq
  }

  private[geotrellis] def getDifferenceByUnit(unit: Int, base: ZonedDateTime, time: ZonedDateTime): Int =
    Math.toIntExact(unit match {
      case UnitSeconds => SECONDS.between(base, time)
      case UnitMinutes => MINUTES.between(base, time)
      case UnitHours   => HOURS.between(base, time)
      case UnitDays    => DAYS.between(base, time)
      case UnitWeeks   => WEEKS.between(base, time)
      case UnitMonths  => MONTHS.between(base, time)
      case UnitYears   => YEARS.between(base, time)
      case _ => throw new IllegalStateException(s"Bad unit $unit.")
    })


  // If the raster local operations doesn't have the operation you need as
  // a operation on tile sequences, just create it through a reduce.

  private[geotrellis] def minReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMin

  private[geotrellis] def maxReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMax

  private[geotrellis] def meanReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMean

  private[geotrellis] def varianceReduceOp(tiles: Traversable[Tile]): Tile = tiles.localVariance

}
