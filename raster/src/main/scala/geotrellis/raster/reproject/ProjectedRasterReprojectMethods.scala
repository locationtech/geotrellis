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

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.util.MethodExtensions

import spire.syntax.cfor._


class ProjectedRasterReprojectMethods[T <: CellGrid](val self: ProjectedRaster[T]) extends MethodExtensions[ProjectedRaster[T]] {
  import Reproject.Options

  def reproject(dest: CRS, options: Options)(implicit ev: Raster[T] => RasterReprojectMethods[Raster[T]]): ProjectedRaster[T] =
    ProjectedRaster(self.raster.reproject(self.crs, dest, options), dest)

  def reproject(dest: CRS)(implicit ev: Raster[T] => RasterReprojectMethods[Raster[T]]): ProjectedRaster[T] =
    reproject(dest, Options.DEFAULT)

  /** Windowed */
  def reproject(gridBounds: GridBounds, dest: CRS, options: Options)(implicit ev: Raster[T] => RasterReprojectMethods[Raster[T]]): ProjectedRaster[T] =
    ProjectedRaster(self.raster.reproject(gridBounds, self.crs, dest, options), dest)

  def reproject(gridBounds: GridBounds, dest: CRS)(implicit ev: Raster[T] => RasterReprojectMethods[Raster[T]]): ProjectedRaster[T] =
    reproject(gridBounds, dest, Options.DEFAULT)

  def regionReproject(dest: CRS, rasterExtent: RasterExtent, resampleMethod: ResampleMethod)
               (implicit ev: RasterRegionReproject[T]): ProjectedRaster[T] = {
    ProjectedRaster(
      raster = ev.regionReproject(
        self.raster, self.crs, dest, rasterExtent,
        self.projectedExtent.reprojectAsPolygon(dest, 0.005),
        resampleMethod),
      crs = dest
    )
  }

  def regionReproject(rasterExtent: ProjectedRasterExtent, resampleMethod: ResampleMethod)
               (implicit ev: RasterRegionReproject[T]): ProjectedRaster[T] = {
    regionReproject(rasterExtent.crs, rasterExtent, resampleMethod)
  }
}
