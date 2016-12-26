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

package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.util._

import scalaz.Functor
import org.apache.spark.rdd._

import java.time.ZonedDateTime

/** See [[geotrellis.spark.filter.ToSpatial]] to get explanations about Metadata (M[K]) constrains */
abstract class SpaceTimeToSpatialMethods[
  K: SpatialComponent: TemporalComponent:  λ[α => Component[M[α], Bounds[α]]],
  V,
  M[_]: Functor
] extends MethodExtensions[RDD[(K, V)] with Metadata[M[K]]] {
  def toSpatial(instant: Long): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self, instant)

  def toSpatial(dateTime: ZonedDateTime): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    toSpatial(dateTime.toInstant.toEpochMilli)
}
