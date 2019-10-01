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
import geotrellis.raster.rasterize._
import geotrellis.vector.Polygon
import geotrellis.proj4._

import spire.syntax.cfor._
import spire.math.Integral

trait MultibandRasterReprojectMethods extends RasterReprojectMethods[MultibandRaster] {

  def reproject(
    transform: Transform,
    inverseTransform: Transform,
    resampleTarget: ResampleTarget
  ): MultibandRaster = {
    val Raster(tile, extent) = self
    val bands =
      for (bandIndex <- 0 until tile.bandCount ) yield
        Raster(tile.band(bandIndex), extent).reproject(transform, inverseTransform, resampleTarget).tile

    val re = ReprojectRasterExtent(self.rasterExtent, transform, resampleTarget).extent
    Raster(ArrayMultibandTile(bands.toArray), re)
  }
}
