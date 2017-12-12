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

package geotrellis.raster.reproject

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.util.MethodExtensions

trait RasterReprojectMethods[+T <: Raster[_]] extends MethodExtensions[T] {
  import Reproject.Options

  def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform, options: Options): T

  def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): T =
    reproject(targetRasterExtent, transform, inverseTransform, Options.DEFAULT)

  def reproject(src: CRS, dest: CRS, options: Options): T =
    if(src == dest) {
      self
    } else {
      val transform = Transform(src, dest)
      val inverseTransform = Transform(dest, src)

      val targetRasterExtent = options.targetRasterExtent.getOrElse(ReprojectRasterExtent(self.rasterExtent, transform, options = options))

      reproject(targetRasterExtent, transform, inverseTransform, options)
    }

  def reproject(src: CRS, dest: CRS): T =
    reproject(src, dest, Options.DEFAULT)

  // Windowed

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): T = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    reproject(gridBounds, transform, inverseTransform, options)
  }

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS): T =
    reproject(gridBounds, src, dest, Options.DEFAULT)

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): T = {
    val rasterExtent = self.rasterExtent
    val windowExtent = rasterExtent.extentFor(gridBounds)
    val windowRasterExtent = RasterExtent(windowExtent, gridBounds.width, gridBounds.height)

    val targetRasterExtent = options.targetRasterExtent.getOrElse(ReprojectRasterExtent(windowRasterExtent, transform, options = options))

    reproject(targetRasterExtent, transform, inverseTransform, options)
  }

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform): T =
    reproject(gridBounds, transform, inverseTransform, Options.DEFAULT)
}
