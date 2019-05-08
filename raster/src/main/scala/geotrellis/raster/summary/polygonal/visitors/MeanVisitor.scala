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

/**
  * Visitor that implements the Mean aggregate function
  *
  * Implementations provided for Raster[(Tile, MultibandTile)]
  *
  * This visitor skips row/col positions with NaN values.
  */
object MeanVisitor {
  implicit def toTileVisitor(t: MeanVisitor.type): TileMeanVisitor = new TileMeanVisitor
  implicit def toMultibandTileVisitor(t: MeanVisitor.type): MultibandTileMeanVisitor =
    new MultibandTileMeanVisitor

  class TileMeanVisitor extends TileFoldingVisitor {
    // Start at 1 because the first time we call fold we'll have already visited n = 1
    private var count: Int = 1

    def fold(mean: Double, newValue: Double): Double = {
      count += 1
      (newValue + (count - 1) * mean) / count
    }
  }

  class MultibandTileMeanVisitor extends MultibandTileFoldingVisitor {
    private var count: Int = 1

    def fold(mean: Double, newValue: Double): Double = {
      count += 1
      (newValue + (count - 1) * mean) / count
    }
  }
}
