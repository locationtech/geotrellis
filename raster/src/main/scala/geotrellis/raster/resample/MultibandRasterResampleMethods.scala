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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import spire.math.Integral

trait MultibandRasterResampleMethods extends RasterResampleMethods[MultibandRaster] {
  def resample(resampleTarget: ResampleTarget, method: ResampleMethod): MultibandRaster = {
    val bandCount = self.tile.bandCount
    val resampledBands = Array.ofDim[Tile](bandCount)
    val targetGrid = resampleTarget(self.rasterExtent)

    cfor(0)(_ < bandCount, _ + 1) { b =>
      resampledBands(b) = Raster(self.tile.band(b), targetGrid.toRasterExtent.extent).resample(resampleTarget, method).tile
    }

    Raster(ArrayMultibandTile(resampledBands), targetGrid.toRasterExtent.extent)
  }
}
