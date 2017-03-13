/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.Partitioner
import geotrellis.util._

import org.apache.spark.rdd.RDD
import jp.ne.opt.chronoscala.Imports._

import java.time._
import java.time.temporal.ChronoUnit._
import scala.reflect.ClassTag

object LocalTemporalStatistics {
  import TemporalWindowHelper._

  def temporalMin[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(minReduceOp)

  def temporalMin[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(minReduceOp)

  def temporalMax[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(maxReduceOp)

  def temporalMax[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(maxReduceOp)

  def temporalMean[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(meanReduceOp)

  def temporalMean[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(meanReduceOp)

  def temporalVariance[K: ClassTag: SpatialComponent: TemporalComponent](
    rdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(rdd, windowSize, unit, start, end, partitioner)(varianceReduceOp)

  def temporalVariance[K: SpatialComponent: TemporalComponent](
    seq: Seq[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime): Seq[(K, Tile)] =
    aggregateWithTemporalWindow(seq, windowSize, unit, start, end)(varianceReduceOp)

  private def aggregateWithTemporalWindow[K: ClassTag: SpatialComponent: TemporalComponent](
    sourceRdd: RDD[(K, Tile)],
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
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

  private def aggregateWithTemporalWindow[K: SpatialComponent: TemporalComponent](
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

  private def getDifferenceByUnit(unit: Int, base: ZonedDateTime, time: ZonedDateTime): Int =
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

  private def minReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMin

  private def maxReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMax

  private def meanReduceOp(tiles: Traversable[Tile]): Tile = tiles.localMean

  private def varianceReduceOp(tiles: Traversable[Tile]): Tile = tiles.localVariance

}
