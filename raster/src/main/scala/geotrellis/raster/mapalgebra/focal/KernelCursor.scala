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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.macros._

class KernelCursor(r: Tile, kernel: Kernel, analysisArea: GridBounds)
    extends Cursor(r, analysisArea, kernel.extent)
    with MacroIterableTile
    with Serializable {
  private val ktileArr = kernel.tile.toArray
  private val ktileArrDouble = kernel.tile.toArrayDouble
  private val kcols = kernel.tile.cols

  def foreachWithWeight(f: (Int, Int, Int) => Unit): Unit =
    macro TileMacros.intForeach_impl

  def foreachWithWeightDouble(f: (Int, Int, Double) => Unit): Unit =
    macro TileMacros.doubleForeach_impl

  def foreachIntVisitor(f: IntTileVisitor): Unit = {
    var y = rowmin
    var x = 0
    while(y <= rowmax) {
      x = colmin
      while(x <= colmax) {
        val kcol = focusCol + extent - x
        val krow = focusRow + extent - y
        val w = ktileArr(krow * kcols + kcol)
        f(x, y, w)
        x += 1
      }
      y += 1
    }
  }

  def foreachDoubleVisitor(f: DoubleTileVisitor): Unit = {
    var y = rowmin
    var x = 0
    val ktile = kernel.tile
    while(y <= rowmax) {
      x = colmin
      while(x <= colmax) {
        val kcol = focusCol + extent - x
        val krow = focusRow + extent - y
        val w = ktileArrDouble(krow * kcols + kcol)
        f(x, y, w)
        x += 1
      }
      y += 1
    }
  }
}
