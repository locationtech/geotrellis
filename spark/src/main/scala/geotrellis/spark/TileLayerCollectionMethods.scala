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

package geotrellis.spark

import geotrellis.raster._
import geotrellis.tiling.SpatialComponent
import geotrellis.util.MethodExtensions

abstract class TileLayerCollectionMethods[K: SpatialComponent] extends MethodExtensions[TileLayerCollection[K]] {
  def convert(cellType: CellType) =
    ContextCollection(
      self.map { case (key, value) => (key, value.convert(cellType)) },
      self.metadata.copy(cellType = cellType))
}
