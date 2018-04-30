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

  def renderAscii(palette: AsciiArtEncoder.Palette = AsciiArtEncoder.Palette.WIDE): String =
    AsciiArtEncoder.encode(self, palette)

  /**
    * Return ascii art of this raster.
    */
  def asciiDraw(): String = NumericEncoder.encodeIntegrals(self)

  /**
    * Return ascii art of this raster. The single int parameter
    * indicates the number of significant digits to be printed.
    */
  def asciiDrawDouble(significantDigits: Int = Int.MaxValue): String =
    NumericEncoder.encodeDecimals(self, significantDigits)

  /**
    * Return ascii art of a range from this raster.
    */
  def asciiDrawRange(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): String =
    NumericEncoder.encodeRange(self, colMin, rowMin, colMax, rowMax)
}
