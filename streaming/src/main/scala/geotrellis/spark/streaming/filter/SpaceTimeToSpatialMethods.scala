package geotrellis.spark.streaming.filter

/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import geotrellis.spark._
import geotrellis.util._
import geotrellis.spark.streaming._
import geotrellis.spark.filter.ToSpatial

import java.time.ZonedDateTime
import org.apache.spark.streaming.dstream.DStream

abstract class SpaceTimeToSpatialMethods[K: SpatialComponent: TemporalComponent, V, M: Component[?, Bounds[K]]]
  extends MethodExtensions[DStream[(K, V)] with Metadata[M]] {
  def toSpatial(instant: Long): DStream[(SpatialKey, V)] with Metadata[M] =
    self.transformWithContext(ToSpatial(_, instant))

  def toSpatial(dateTime: ZonedDateTime): DStream[(SpatialKey, V)] with Metadata[M] =
    toSpatial(dateTime.toInstant.toEpochMilli)
}
