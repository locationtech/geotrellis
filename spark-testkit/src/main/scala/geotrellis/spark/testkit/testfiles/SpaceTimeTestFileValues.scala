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

package geotrellis.spark.testkit.testfiles

import geotrellis.spark._
import geotrellis.raster._

import spire.syntax.cfor._

abstract class TestFileSpaceTimeTiles(tileLayout: TileLayout) {
  final def apply(key: SpaceTimeKey, timeIndex: Int): Tile = {
    val tile = FloatArrayTile.empty(tileLayout.tileCols, tileLayout.tileRows)

    cfor(0)(_ < tileLayout.tileRows, _ + 1) { row =>
      cfor(0)(_ < tileLayout.tileCols, _ + 1) { col =>
        tile.setDouble(col, row, value(key, timeIndex, col, row))
      }
    }

    tile
  }

  def value(key: SpaceTimeKey, timeIndex: Int, col: Int, row: Int): Double
}


class ConstantSpaceTimeTestTiles(tileLayout: TileLayout, v: Double) extends TestFileSpaceTimeTiles(tileLayout) {
  def value(key: SpaceTimeKey, timeIndex: Int, col: Int, row: Int): Double = v
}

/** Coordinates are CCC,RRR.TTT where C = column, R = row, T = time (year in 2010 + T).
  * So 34,025.004 would represent col 34, row 25, year 2014
  */
class CoordinateSpaceTimeTestTiles(tileLayout: TileLayout) extends TestFileSpaceTimeTiles(tileLayout) {
  def value(key: SpaceTimeKey, timeIndex: Int, col: Int, row: Int): Double= {
    val SpaceTimeKey(layoutCol, layoutRow, _) = key
    (layoutCol * 1000.0) + layoutRow + (timeIndex / 1000.0)
  }
}
