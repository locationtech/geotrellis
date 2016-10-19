/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.raster.sigmoidal

import geotrellis.raster._
import geotrellis.raster.histogram._


object SigmoidalContrast {

  private def _T(
    cellType: CellType, alpha: Double, beta: Double
  )(_u: Double): Double = {
    val bits = cellType.bits

    val u = cellType match {
      case _: FloatCells =>
        (_u - Float.MinValue)/(Float.MaxValue - Float.MinValue)
      case _: DoubleCells =>
        (_u/2 - Double.MinValue/2)/(Double.MaxValue/2 - Double.MinValue/2)
      case _: BitCells | _: UByteCells | _: UShortCells =>
        (_u / ((1<<bits)-1))
      case _: ByteCells | _: ShortCells | _: IntCells =>
        (_u + (1<<(bits-1))) / ((1<<bits)-1)
    }

    val numer = 1/(1+math.exp(beta*(alpha-u))) - 1/(1+math.exp(beta))
    val denom = 1/(1+math.exp(beta*(alpha-1))) - 1/(1+math.exp(beta*alpha))
    val gu = math.max(0.0, math.min(1.0, numer / denom))

    cellType match {
      case _: FloatCells =>
        (Float.MaxValue * (2*gu - 1.0))
      case _: DoubleCells =>
        (Double.MaxValue * (2*gu - 1.0))
      case _: BitCells | _: UByteCells | _: UShortCells =>
        ((1<<bits) - 1) * gu
      case _: ByteCells | _: ShortCells | _: IntCells =>
        (((1<<bits) - 1) * gu) - (1<<(bits-1))
    }
  }

  def apply(tile: Tile, alpha: Double, beta: Double): Tile = {
    val T = _T(tile.cellType, alpha, beta)_
    tile.mapDouble(T)
  }

  def apply(tile: MultibandTile, alpha: Double, beta: Double): MultibandTile = {
    val T = _T(tile.cellType, alpha, beta)_
    MultibandTile(tile.bands.map(_.mapDouble(T)))
  }

}
