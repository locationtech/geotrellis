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

trait MultibandRasterResampleMethods extends RasterResampleMethods[MultibandRaster] {
  def resample(target: RasterExtent, method: ResampleMethod): MultibandRaster = {
    val tile = self.tile
    val extent = self.extent
    val bandCount = tile.bandCount
    val resampledBands = Array.ofDim[Tile](bandCount)

    cfor(0)(_ < bandCount, _ + 1) { b =>
      resampledBands(b) = Raster(tile.band(b), extent).resample(target, method).tile
    }

    Raster(ArrayMultibandTile(resampledBands), target.extent)
  }
}
