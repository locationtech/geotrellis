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
import geotrellis.proj4._
import geotrellis.vector.Extent

import spire.math.Integral

trait SinglebandTileReprojectMethods extends TileReprojectMethods[Tile] {

  def reproject(srcExtent: Extent, transform: Transform, inverseTransform: Transform, resampleTarget: ResampleTarget): Raster[Tile] =
    Raster(self, srcExtent).reproject(transform, inverseTransform, resampleTarget)

  def reproject(srcExtent: Extent, src: CRS, dest: CRS, resampleTarget: ResampleTarget): Raster[Tile] =
    Raster(self, srcExtent).reproject(src, dest, resampleTarget)

}
