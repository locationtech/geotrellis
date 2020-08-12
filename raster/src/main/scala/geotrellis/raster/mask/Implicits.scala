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

object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandTileMaskMethods(val self: Tile) extends SinglebandTileMaskMethods
  implicit class withMultibandTileMaskMethods(val self: MultibandTile) extends MultibandTileMaskMethods

  implicit class withSinglebandRasterMaskMethods(self: Raster[Tile]) extends RasterMaskMethods[Tile](self)
  implicit class withMultibandRasterMaskMethods(self: Raster[MultibandTile]) extends RasterMaskMethods[MultibandTile](self)

  implicit class withSinglebandTileFeatureMaskMethods[D](self: TileFeature[Tile, D]) extends TileFeatureMaskMethods[Tile, D](self)
  implicit class withMultibandTileFeatureMaskMethods[D](self: TileFeature[MultibandTile, D]) extends TileFeatureMaskMethods[MultibandTile, D](self)

  implicit class withSinglebandRasterTileFeatureMaskMethods[D](self: TileFeature[Raster[Tile], D]) extends RasterTileFeatureMaskMethods[Tile, D](self)
  implicit class withMultibandRasterTileFeatureMaskMethods[D](self: TileFeature[Raster[MultibandTile], D]) extends RasterTileFeatureMaskMethods[MultibandTile, D](self)
}
