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

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._

object Mask extends Serializable {
  /**
   * Generate a raster with the values from the first raster, but only include
   * cells in which the corresponding cell in the second raster *are not* set to the
   * "readMask" value.
   *
   * For example, if *all* cells in the second raster are set to the readMask value,
   * the output raster will be empty -- all values set to NODATA.
   */
  def apply(r1: Tile, r2: Tile, readMask: Int, writeMask: Int): Tile = {
    val out = ArrayTile.alloc(r1.cellType, r1.cols, r1.rows)
    if (r1.cellType.isFloatingPoint) {
      ArrayTile.combineDouble(r1, r2, out, { (v: Double, m: Double) => if (d2i(m) == readMask) i2d(writeMask) else v })
    } else {
      ArrayTile.combine(r1, r2, out, { (v: Int, m: Int) => if (m == readMask) writeMask else v })
    }
    out
  }
}
