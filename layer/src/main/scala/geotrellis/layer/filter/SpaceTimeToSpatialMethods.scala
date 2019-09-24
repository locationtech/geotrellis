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

package geotrellis.layer.filter

import geotrellis.layer._
import geotrellis.util._

import cats.Functor
import java.time.ZonedDateTime

/** See [[geotrellis.layer.filter.ToSpatial]] to get explanations about Metadata (M[K]) constrains */
abstract class SpaceTimeToSpatialMethods[
  K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
  V,
  M[_]: Functor
] extends MethodExtensions[Seq[(K, V)] with Metadata[M[K]]] {
  def toSpatial(instant: Long): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self, instant)

  def toSpatial(dateTime: ZonedDateTime): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    toSpatial(dateTime.toInstant.toEpochMilli)

  def toSpatial(): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self)
}

abstract class SpaceTimeToSpatialReduceMethods[
  K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
  V,
  M[_]: Functor
] extends MethodExtensions[Seq[(K, V)] with Metadata[M[K]]] {
  def toSpatialReduce(mergeFunc: (V, V) => V): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self, Some(mergeFunc))
}
