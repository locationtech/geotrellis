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

package geotrellis.raster.split

import geotrellis.raster.{Tile, MultibandTile, RasterExtent, Raster, TileFeature}

object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandTileSplitMethods[D](val self: Tile) extends SinglebandTileSplitMethods
  implicit class withMultibandTileSplitMethods[D](val self: MultibandTile) extends MultibandTileSplitMethods

  implicit class withRasterExtentSplitMethods(val self: RasterExtent) extends RasterExtentSplitMethods

  implicit class withSinglebandRasterSplitMethods(val self: Raster[Tile]) extends SinglebandRasterSplitMethods
  implicit class withMultibandRasterSplitMethods(val self: Raster[MultibandTile]) extends MultibandRasterSplitMethods

  implicit class withSinglebandTileFeatureSplitMethods[D](val self: TileFeature[Tile, D]) extends SinglebandTileFeatureSplitMethods[D]
  implicit class withMultibandTileFeatureSplitMethods[D](val self: TileFeature[MultibandTile, D]) extends MultibandTileFeatureSplitMethods[D]
}
