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

package geotrellis.raster.merge

import geotrellis.raster._
import cats.Semigroup

object Implicits extends Implicits

/**
  * A trait holding the implicit class which makes the extensions
  * methods available.
  */
trait Implicits {
  implicit class withSinglebandMergeMethods(val self: Tile) extends SinglebandTileMergeMethods
  implicit class withMultibandMergeMethods(val self: MultibandTile) extends MultibandTileMergeMethods

  implicit class withSinglebandRasterMergeMethod(val self: Raster[Tile]) extends SinglebandRasterMergeMethods
  implicit class withMultibandRasterMergeMethod(val self: Raster[MultibandTile]) extends MultibandRasterMergeMethods

  implicit class withSinglebandTileFeatureMergeMethods[D: Semigroup](val self: TileFeature[Tile, D]) extends SinglebandTileFeatureMergeMethods[D]
  implicit class withMultibandTileFeatureMergeMethods[D: Semigroup](val self: TileFeature[MultibandTile, D]) extends MultibandTileFeatureMergeMethods[D]

  implicit class withSinglebandRasterTileFeatureMergeMethods[D: Semigroup](val self: TileFeature[Raster[Tile], D]) extends SinglebandRasterTileFeatureMergeMethods[D]
  implicit class withMultibandRasterTileFeatureMergeMethods[D: Semigroup](val self: TileFeature[Raster[MultibandTile], D]) extends MultibandRasterTileFeatureMergeMethods[D]
}
