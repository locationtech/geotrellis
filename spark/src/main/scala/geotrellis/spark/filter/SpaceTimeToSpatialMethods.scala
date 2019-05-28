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

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import cats.Functor
import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import java.time.ZonedDateTime

import geotrellis.layers.Metadata

import scala.reflect.ClassTag


/** See [[geotrellis.spark.filter.ToSpatial]] to get explanations about Metadata (M[K]) constrains */
abstract class SpaceTimeToSpatialMethods[
  K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
  V,
  M[_]: Functor
] extends MethodExtensions[RDD[(K, V)] with Metadata[M[K]]] {
  def toSpatial(instant: Long): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self, instant)

  def toSpatial(dateTime: ZonedDateTime): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    toSpatial(dateTime.toInstant.toEpochMilli)

  def toSpatial(): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self)
}

/**
  * This exists because `reduceByKey` needs K and V to be members of
  * the `ClassTag` type class, but adding that restriction to the type
  * parameters of the class above would break the API.  Neither method
  * can be named `toSpatial` as that would hide the methods provided
  * by the class above.
  */
abstract class SpaceTimeToSpatialReduceMethods[
  K: ClassTag: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
  V: ClassTag,
  M[_]: Functor
] extends MethodExtensions[RDD[(K, V)] with Metadata[M[K]]] {

  def toSpatialReduce(
    mergeFunc: (V, V) => V
  ): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self, Some(mergeFunc), None)

  def toSpatialReduce(
    mergeFunc: (V, V) => V,
    partitioner: Partitioner
  ): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] =
    ToSpatial(self, Some(mergeFunc), Some(partitioner))

}
