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

package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.util.MethodExtensions

abstract class InverseDistanceWeightedMethods[D](implicit ev: D => Double) extends MethodExtensions[Traversable[PointFeature[D]]] {
  def inverseDistanceWeighted(rasterExtent: RasterExtent, options: InverseDistanceWeighted.Options): Raster[Tile] =
    InverseDistanceWeighted(self, rasterExtent, options)

  def inverseDistanceWeighted(rasterExtent: RasterExtent): Raster[Tile] =
    inverseDistanceWeighted(rasterExtent, InverseDistanceWeighted.Options.DEFAULT)
}
