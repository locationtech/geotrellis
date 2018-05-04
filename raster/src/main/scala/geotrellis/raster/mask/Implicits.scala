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

package geotrellis.raster.mask

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector.Geometry

object Implicits extends Implicits

trait Implicits {
  implicit class withRasterMaskMethods[T <: CellGrid: (? => TileMaskMethods[T])](val self: Raster[T]) extends RasterMaskMethods[T] {
    /**
      * Masks this raster by the given Geometry.
      */
    def mask(geoms: Traversable[Geometry], options: Options): Raster[T] =
      self.mapTile(_.mask(self.extent, geoms, options))
  }

  implicit class withRasterTileFeatureMaskMethods[T <: CellGrid: (? => TileMaskMethods[T]), D](val self: TileFeature[Raster[T], D]) extends RasterTileFeatureMaskMethods[T, D](self)

  implicit class withTileFeatureMaskMethods[T <: CellGrid : (? => TileMaskMethods[T]), D](override val self: TileFeature[T, D]) extends TileFeatureMaskMethods[T, D](self)

}
