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

object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandReprojectMethods(val self: Tile) extends SinglebandTileReprojectMethods
  implicit class withMultibandReprojectMethods(val self: MultibandTile) extends MultibandTileReprojectMethods

  implicit class withSinglebandRasterReprojectMethods(val self: Raster[Tile]) extends SinglebandRasterReprojectMethods
  implicit class withMultibandRasterReprojectMethods(val self: Raster[MultibandTile]) extends MultibandRasterReprojectMethods

  implicit class withSinglebandProjectedRasterReprojectMethods(self: ProjectedRaster[Tile]) extends ProjectedRasterReprojectMethods[Tile](self)
  implicit class withMultibandProjectedRasterReprojectMethods(self: ProjectedRaster[MultibandTile]) extends ProjectedRasterReprojectMethods[MultibandTile](self)

  implicit class withSinglebandTileFeatureReprojectMethods[D](self: TileFeature[Tile, D]) extends TileFeatureReprojectMethods[Tile, D](self)
  implicit class withMultibandTileFeatureReprojectMethods[D](self: TileFeature[MultibandTile, D]) extends TileFeatureReprojectMethods[MultibandTile, D](self)

  implicit class withSinglebandRasterTileFeatureReprojectMethods[D](self: TileFeature[Raster[Tile], D]) extends RasterTileFeatureReprojectMethods[Tile, D](self)
  implicit class withMultibandRasterTileFeatureReprojectMethods[D](self: TileFeature[Raster[MultibandTile], D]) extends RasterTileFeatureReprojectMethods[MultibandTile, D](self)
}
