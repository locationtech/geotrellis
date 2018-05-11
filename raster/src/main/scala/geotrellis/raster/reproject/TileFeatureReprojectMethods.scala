/*
 * Copyright 2017 Azavea
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
import geotrellis.vector._
import geotrellis.util.MethodExtensions

class TileFeatureReprojectMethods[
  T <: CellGrid : (? => TileReprojectMethods[T]), 
  D
](val self: TileFeature[T, D]) extends TileReprojectMethods[TileFeature[T, D]] {
  import Reproject.Options

  def reproject(srcExtent: Extent, 
                targetRasterExtent: RasterExtent, 
                transform: Transform, 
                inverseTransform: Transform, 
                options: Options): Raster[TileFeature[T, D]] = {
    val Raster(tile, extent) = self.tile.reproject(srcExtent, targetRasterExtent, transform, inverseTransform, options)
    Raster(TileFeature(tile, self.data), extent)
  }

  def reproject(srcExtent: Extent, src: CRS, dest: CRS, options: Options): Raster[TileFeature[T, D]] = {
    val Raster(tile, extent) = self.tile.reproject(srcExtent, src, dest, options)
    Raster(TileFeature(tile, self.data), extent)
  }

  def reproject(srcExtent: Extent, gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): Raster[TileFeature[T, D]] = {
    val Raster(tile, extent) = self.tile.reproject(srcExtent, gridBounds, src, dest, options)
    Raster(TileFeature(tile, self.data), extent)
  }

  def reproject(srcExtent: Extent, 
                gridBounds: GridBounds, 
                transform: Transform, 
                inverseTransform: Transform, 
                options: Options): Raster[TileFeature[T, D]] = {
    val Raster(tile, extent) = self.tile.reproject(srcExtent, gridBounds, transform, inverseTransform, options)
    Raster(TileFeature(tile, self.data), extent)
  }
}

class RasterTileFeatureReprojectMethods[
  T <: CellGrid : (? => TileReprojectMethods[T]), 
  D
](val self: TileFeature[Raster[T], D]) extends MethodExtensions[TileFeature[Raster[T], D]] {
  import Reproject.Options

  def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform, options: Options): TileFeature[Raster[T], D] =
    TileFeature(self.tile.tile.reproject(self.tile.extent, targetRasterExtent, transform, inverseTransform, options), self.data)

  def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): TileFeature[Raster[T], D] =
    reproject(targetRasterExtent, transform, inverseTransform, Options.DEFAULT)

  def reproject(src: CRS, dest: CRS, options: Options): TileFeature[Raster[T], D] =
    TileFeature(self.tile.tile.reproject(self.tile.extent, src, dest, options), self.data)

  def reproject(src: CRS, dest: CRS): TileFeature[Raster[T], D] =
    reproject(src, dest, Options.DEFAULT)

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): TileFeature[Raster[T], D] = 
    TileFeature(self.tile.tile.reproject(self.tile.extent, gridBounds, src, dest, options), self.data)

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS): TileFeature[Raster[T], D] =
    reproject(gridBounds, src, dest, Options.DEFAULT)

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): TileFeature[Raster[T], D] = 
    TileFeature(self.tile.tile.reproject(self.tile.extent, gridBounds, transform, inverseTransform, options), self.data)

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform): TileFeature[Raster[T], D] =
    reproject(gridBounds, transform, inverseTransform, Options.DEFAULT)
}
