/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.summary.polygonal.visitors

import geotrellis.raster._

/**
  * Visitor that implements the Max aggregate function
  *
  * Implementations provided for Raster[(Tile, MultibandTile)]
  *
  * This visitor skips row/col positions with NaN values.
  */
object MaxVisitor {
  implicit def toTileVisitor(t: MaxVisitor.type): TileMaxVisitor = new TileMaxVisitor
  implicit def toMultibandTileVisitor(t: MaxVisitor.type): MultibandTileMaxVisitor =
    new MultibandTileMaxVisitor

  class TileMaxVisitor extends TileFoldingVisitor {
    def fold(max: Double, newValue: Double): Double =
      if (isData(newValue) && newValue > max) newValue else max
  }

  class MultibandTileMaxVisitor extends MultibandTileFoldingVisitor {
    def fold(max: Double, newValue: Double): Double =
      if (isData(newValue) && newValue > max) newValue else max
  }
}
