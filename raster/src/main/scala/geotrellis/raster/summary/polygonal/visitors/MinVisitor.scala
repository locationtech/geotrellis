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
  * Visitor that implements the Min aggregate function
  *
  * Implementations provided for Raster[(Tile, MultibandTile)]
  *
  * This visitor skips row/col positions with NaN values.
  */
object MinVisitor {
  implicit def toTileVisitor(t: MinVisitor.type): TileMinVisitor = new TileMinVisitor
  implicit def toMultibandTileVisitor(t: MinVisitor.type): MultibandTileMinVisitor =
    new MultibandTileMinVisitor

  class TileMinVisitor extends TileFoldingVisitor {
    def fold(min: Double, newValue: Double): Double =
      if (isData(newValue) && newValue < min) newValue else min
  }

  class MultibandTileMinVisitor extends MultibandTileFoldingVisitor {
    def fold(min: Double, newValue: Double): Double =
      if (isData(newValue) && newValue < min) newValue else min
  }
}
