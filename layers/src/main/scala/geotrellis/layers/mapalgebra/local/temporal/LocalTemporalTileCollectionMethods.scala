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

package geotrellis.layers.mapalgebra.local.temporal

import java.time.ZonedDateTime

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.util.MethodExtensions

abstract class LocalTemporalTileCollectionMethods[K: SpatialComponent: TemporalComponent](val self: Seq[(K, Tile)])
    extends MethodExtensions[Seq[(K, Tile)]] {

  def temporalMin(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime
  ): Seq[(K, Tile)] =
    LocalTemporalStatistics.temporalMin(self, windowSize, unit, start, end)

  def temporalMax(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime
  ): Seq[(K, Tile)] =
    LocalTemporalStatistics.temporalMax(self, windowSize, unit, start, end)

  def temporalMean(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime
  ): Seq[(K, Tile)] =
    LocalTemporalStatistics.temporalMean(self, windowSize, unit, start, end)

  def temporalVariance(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime
  ): Seq[(K, Tile)] =
    LocalTemporalStatistics.temporalVariance(self, windowSize, unit, start, end)
}
