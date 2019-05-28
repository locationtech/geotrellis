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

package geotrellis.layers.mask

import geotrellis.tiling._
import geotrellis.raster.mask.TileMaskMethods
import geotrellis.layers.Metadata
import geotrellis.util._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withTileCollectionMaskMethods[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](val self: Seq[(K, V)] with Metadata[M]) extends TileCollectionMaskMethods[K, V, M]
}
