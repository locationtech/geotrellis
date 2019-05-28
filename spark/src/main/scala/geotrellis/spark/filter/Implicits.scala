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
import geotrellis.layers.Metadata
import org.apache.spark.rdd._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withTileLayerRDDFilterMethods[
    K: Boundable,
    V,
    M: Component[?, Bounds[K]]
  ](val self: RDD[(K, V)] with Metadata[M])
      extends TileLayerRDDFilterMethods[K, V, M]

  implicit class withSpaceTimeToSpatialMethods[
    K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]: Functor
  ](val self: RDD[(K, V)] with Metadata[M[K]]) extends SpaceTimeToSpatialMethods[K, V, M]

  implicit class withSpaceTimeToSpatialReduceMethods[
    K: ClassTag: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V: ClassTag,
    M[_]: Functor
  ](val self: RDD[(K, V)] with Metadata[M[K]]) extends SpaceTimeToSpatialReduceMethods[K, V, M]
}
