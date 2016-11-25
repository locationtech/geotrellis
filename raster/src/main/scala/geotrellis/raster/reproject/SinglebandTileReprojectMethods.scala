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

trait SinglebandTileReprojectMethods extends TileReprojectMethods[Tile] {
  import Reproject.Options

  def reproject(srcExtent: Extent, targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform, options: Options): Raster[Tile] =
    Raster(self, srcExtent).reproject(targetRasterExtent, transform, inverseTransform, options)

  def reproject(srcExtent: Extent, src: CRS, dest: CRS, options: Options): Raster[Tile] =
    Raster(self, srcExtent).reproject(src, dest, options)

  def reproject(srcExtent: Extent, gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): Raster[Tile] =
    Raster(self, srcExtent).reproject(gridBounds, src, dest, options)

  def reproject(srcExtent: Extent, gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): Raster[Tile] =
    Raster(self, srcExtent).reproject(gridBounds, transform, inverseTransform, options)
}
