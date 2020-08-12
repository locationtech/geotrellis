/*
 * Copyright 2016-2017 Azavea
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

package geotrellis.raster.crop

import geotrellis.raster._


object Implicits extends Implicits

/**
  * Trait housing the implicit class which add extension methods for
  * cropping to [[CellGrid]].
  */
trait Implicits {
  implicit class withSinglebandTileCropMethods(val self: Tile) extends SinglebandTileCropMethods
  implicit class withMultibandTileCropMethods(val self: MultibandTile) extends MultibandTileCropMethods

  implicit class withSinglebandTileRasterCropMethods(val self: Raster[Tile]) extends RasterCropMethods[Tile](self)
  implicit class withMultibandTileRasterCropMethods(val self: Raster[MultibandTile]) extends RasterCropMethods[MultibandTile](self)

  implicit class withSinglebandTileFeatureCropMethods[D](self: TileFeature[Tile, D]) extends TileFeatureCropMethods[Tile, D](self)
  implicit class withMultibandTileFeatureCropMethods[D](self: TileFeature[MultibandTile, D]) extends TileFeatureCropMethods[MultibandTile, D](self)

  implicit class withSinglebandRasterTileFeatureCropMethods[D](self: TileFeature[Raster[Tile], D]) extends RasterTileFeatureCropMethods[Tile, D](self)
  implicit class withMultibandRasterTileFeatureCropMethods[D](self: TileFeature[Raster[MultibandTile], D]) extends RasterTileFeatureCropMethods[MultibandTile, D](self)
}
