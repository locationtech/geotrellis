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
  implicit class withSinglebandTileMaskMethods(val self: Tile) extends SinglebandTileMaskMethods
  implicit class withMultibandTileMaskMethods(val self: MultibandTile) extends MultibandTileMaskMethods

  implicit class withSinglebandRasterMaskMethods(val self: Raster[Tile]) extends SinglebandRasterMaskMethods
  implicit class withMultibandRasterMaskMethods(val self: Raster[MultibandTile]) extends MultibandRasterMaskMethods

  implicit class withSinglebandTileFeatureMaskMethods[D](val self: TileFeature[Tile, D]) extends SinglebandTileFeatureMaskMethods[D]
  implicit class withMultibandTileFeatureMaskMethods[D](val self: TileFeature[MultibandTile, D]) extends MultibandTileFeatureMaskMethods[D]

  implicit class withSinglebandRasterTileFeatureMaskMethods[D](val self: TileFeature[Raster[Tile], D]) extends SinglebandRasterTileFeatureMaskMethods[D]
  implicit class withMultibandRasterTileFeatureMaskMethods[D](val self: TileFeature[Raster[MultibandTile], D]) extends MultibandRasterTileFeatureMaskMethods[D]
}
