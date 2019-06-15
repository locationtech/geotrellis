/*
 * Copyright 2019 Azavea
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

package geotrellis.spark.mapalgebra.local

import geotrellis.layer._
import geotrellis.raster._
import geotrellis.layer.mapalgebra.local.temporal.LocalTemporalStatistics
import geotrellis.util._

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import jp.ne.opt.chronoscala.Imports._
import java.time._

import scala.reflect.ClassTag


package object temporal extends Implicits {
  private[temporal] def aggregateWithTemporalWindow[K: ClassTag: SpatialComponent: TemporalComponent](
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
          val startDiff = LocalTemporalStatistics.getDifferenceByUnit(unit, start, time)
          val endDiff = LocalTemporalStatistics.getDifferenceByUnit(unit, time, end)

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
}
