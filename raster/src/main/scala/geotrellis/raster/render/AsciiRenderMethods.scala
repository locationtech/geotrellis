/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.render

import geotrellis.raster.Tile
import geotrellis.raster.render.ascii.{AsciiArtEncoder, NumericEncoder}
import geotrellis.util.MethodExtensions

/**
 * Extension methods on [[Tile]] for printing a representation as ASCII
 * ranged characters or numerical values.
 * @since 9/6/17
 */
trait AsciiRenderMethods extends MethodExtensions[Tile] {

  def renderAscii(settings: AsciiArtEncoder.Settings = AsciiArtEncoder.Settings()): String =
    AsciiArtEncoder.encode(self, settings)

  /**
   * Return ASCII representation of the tile, emitting a string matrix of
   * integer values corresponding to the cell values.
   */
  def asciiDraw(): String = NumericEncoder.integralEncode(self)

  /**
   * Return ASCII string representation of the tile. The single Int parameter
   * indicates the number of significant digits to be printed.
   */
  def asciiDrawDouble(significantDigits: Int = Int.MaxValue): String =
    NumericEncoder.decimalEncode(self, significantDigits)

  /**
   * Returns an ASCII string representation of a subset of the tile, with cell
   * values encoded as hexadecimal integers.
   */
  def asciiDrawRange(colMin: Int, colMax: Int, rowMin: Int, rowMax: Int): String =
    NumericEncoder.rangeEncode(self, colMin, colMax, rowMin, rowMax)
}
