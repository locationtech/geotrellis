/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.global

import geotrellis._
import geotrellis.raster._

import spire.syntax.cfor._

/**
 * Flip the data for a raster along the X-axis.
 *
 * The geographic extent will remain unchanged.
 *
 * @note    VerticalFlip does not currently support Double raster data.
 *          If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *          the data values will be rounded to integers.
 */
object VerticalFlip {
  def apply(tile: Tile): Tile = {
    val (cols, rows) = tile.dimensions
    val data2 = Array.ofDim[Int](cols * rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      val flipRow = rows - row - 1
      cfor(0)(_ < cols, _ + 1) { col =>
        data2(flipRow * cols + col) = tile.get(col, row)
      }
    }

    ArrayTile(data2, cols, rows)
  }
}
