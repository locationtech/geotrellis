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

package geotrellis.layer.filter

import cats.Functor
import geotrellis.layer._
import geotrellis.util._

object Implicits extends Implicits

trait Implicits {
  implicit class withSpaceTimeToSpatialMethods[
    K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]: Functor
  ](val self: Seq[(K, V)] with Metadata[M[K]]) extends SpaceTimeToSpatialMethods[K, V, M]

  implicit class withSpaceTimeToSpatialReduceMethods[
    K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]: Functor
  ](val self: Seq[(K, V)] with Metadata[M[K]]) extends SpaceTimeToSpatialReduceMethods[K, V, M]
}
