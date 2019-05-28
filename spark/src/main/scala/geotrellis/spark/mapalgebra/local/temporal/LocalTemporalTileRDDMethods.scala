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

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.layers.mapalgebra.local.temporal.LocalTemporalStatistics
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import jp.ne.opt.chronoscala.Imports._
import java.time.ZonedDateTime

import scala.reflect.ClassTag


abstract class LocalTemporalTileRDDMethods[K: ClassTag: SpatialComponent: TemporalComponent](val self: RDD[(K, Tile)])
    extends MethodExtensions[RDD[(K, Tile)]] {
  def temporalMin(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): RDD[(K, Tile)] =
      aggregateWithTemporalWindow(self, windowSize, unit, start, end, partitioner)(LocalTemporalStatistics.minReduceOp)

  def temporalMax(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(self, windowSize, unit, start, end, partitioner)(LocalTemporalStatistics.maxReduceOp)

  def temporalMean(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(self, windowSize, unit, start, end, partitioner)(LocalTemporalStatistics.meanReduceOp)

  def temporalVariance(
    windowSize: Int,
    unit: Int,
    start: ZonedDateTime,
    end: ZonedDateTime,
    partitioner: Option[Partitioner] = None
  ): RDD[(K, Tile)] =
    aggregateWithTemporalWindow(self, windowSize, unit, start, end, partitioner)(LocalTemporalStatistics.varianceReduceOp)
}
