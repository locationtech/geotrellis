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

package geotrellis.spark.streaming.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra.local.temporal.LocalTemporalStatistics
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.streaming.dstream.DStream
import java.time.ZonedDateTime

import scala.reflect.ClassTag

abstract class LocalTemporalTileDStreamMethods[K: ClassTag: SpatialComponent: TemporalComponent](val self: DStream[(K, Tile)])
  extends MethodExtensions[DStream[(K, Tile)]] {

  def temporalMin(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): DStream[(K, Tile)] =
    self.transform(LocalTemporalStatistics.temporalMin(_, windowSize, unit, start, end, partitioner))

  def temporalMax(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): DStream[(K, Tile)] =
    self.transform(LocalTemporalStatistics.temporalMax(_, windowSize, unit, start, end, partitioner))

  def temporalMean(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): DStream[(K, Tile)] =
    self.transform(LocalTemporalStatistics.temporalMean(_, windowSize, unit, start, end, partitioner))

  def temporalVariance(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): DStream[(K, Tile)] =
    self.transform(LocalTemporalStatistics.temporalVariance(_, windowSize, unit, start, end, partitioner))
}
