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

package geotrellis.raster.density

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Kernel
import geotrellis.util.MethodExtensions
import geotrellis.vector._

trait DoubleKernelDensityMethods extends MethodExtensions[Traversable[PointFeature[Double]]] {
  def kernelDensity(kernel: Kernel, rasterExtent: RasterExtent): Tile =
    KernelDensity(self, kernel, rasterExtent)

  def kernelDensity(kernel: Kernel, rasterExtent: RasterExtent, cellType: CellType): Tile =
    KernelDensity(self, kernel, rasterExtent, cellType)
}
