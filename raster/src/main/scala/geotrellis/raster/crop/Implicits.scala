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
import geotrellis.vector._
import geotrellis.util.MethodExtensions


object Implicits extends Implicits

/**
  * Trait housing the implicit class which add extension methods for
  * cropping to [[CellGrid]].
  */
trait Implicits {
  implicit class withTileRasterCropMethods(val self: Raster[Tile]) extends RasterCropMethods[Tile]

  implicit class withMultibandTileRasterCropMethods(val self: Raster[MultibandTile]) extends RasterCropMethods[MultibandTile]

  implicit class withTileFeatureCropMethods[D](val self: TileFeature[Tile, D]) extends TileFeatureCropMethods[D]

  implicit class withSinglebandRasterTileFeatureCropMethods[D](val self: TileFeature[Raster[Tile], D]) extends SinglebandRasterTileFeatureCropMethods[D]

  implicit class withMultibandTileFeatureCropMethods[D](val self: TileFeature[MultibandTile, D]) extends MultibandTileFeatureCropMethods[D]

  implicit class withMultibandRasterTileFeatureCropMethods[D](val self: TileFeature[Raster[MultibandTile], D]) extends MultibandRasterTileFeatureCropMethods[D]

  implicit class withTileCropMethods(val self: Tile) extends SinglebandTileCropMethods

  implicit class withMultibandTileCropMethods(val self: MultibandTile) extends MultibandTileCropMethods
}
