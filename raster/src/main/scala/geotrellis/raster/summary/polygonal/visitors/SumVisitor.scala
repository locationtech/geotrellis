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

import geotrellis.raster.summary.types.SumValue

/**
  * Visitor that implements the Sum aggregate function
  *
  * Implementations provided for Raster[(Tile, MultibandTile)]
  *
  * This visitor skips row/col positions with NaN values.
  */
object SumVisitor {
  implicit def toTileVisitor(t: SumVisitor.type): TileSumVisitor = new TileSumVisitor
  implicit def toMultibandTileVisitor(t: SumVisitor.type): MultibandTileSumVisitor =
    new MultibandTileSumVisitor

  class TileSumVisitor extends TileCombineVisitor[SumValue] {
    def fromDouble(value: Double): SumValue = SumValue(value)
  }

  class MultibandTileSumVisitor extends MultibandTileCombineVisitor[SumValue] {
    def fromDouble(value: Double): SumValue = SumValue(value)
  }
}
