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

/**
 * Flip the data for a raster along the X-axis.
 *
 * The geographic extent will remain unchanged.
 *
 * @note    VerticalFlip does not currently support Double raster data.
 *          If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *          the data values will be rounded to integers.
 */
case class VerticalFlip(r: Op[Tile]) extends Op1(r) ({
  r =>
    val (cols, rows) = r.dimensions
    val data = r.toArray
    val data2 = Array.ofDim[Int](data.size)
 
    var y = 0
    var x = 0
    while (y < rows) {
      x = 0
      val yspan = y * cols
      val yspan2 = (cols - 1 - y) * cols
      while (x < cols) {
        data2(yspan2 + x) = data(yspan + x)
        x += 1
      }
      y += 1
    }
    Result(ArrayTile(data2, cols, rows))
})
