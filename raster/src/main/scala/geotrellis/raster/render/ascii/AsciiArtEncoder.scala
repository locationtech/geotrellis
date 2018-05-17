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

package geotrellis.raster.render.ascii

import geotrellis.raster.Tile
import geotrellis.raster.render.{ColorMap, ColorRamp}
import spire.syntax.cfor.cfor

/**
 * Routines for converting tiles into ASCII art based on cell values.
 *
 * @author sfitch
 * @since 9/6/17
 */
object AsciiArtEncoder {

  /** Convert the given tile into an ASCII art string using the given palette of
   * character codes as fill values. */
  def encode(tile: Tile, palette: AsciiArtEncoder.Palette): String = {
    val palSize = palette.length
    val opts = ColorMap.Options.DEFAULT
      .copy(noDataColor = palette.nodata)
    val colorMap = if(tile.cellType.isFloatingPoint) {
     val hist = tile.histogramDouble(palSize)
     ColorMap.fromQuantileBreaks(hist, palette.colorRamp, opts)
    }
    else {
     val hist = tile.histogram
     ColorMap.fromQuantileBreaks(hist, palette.colorRamp, opts)
    }

    val intEncoded = colorMap.render(tile)

    val sb = new StringBuilder

    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val p = intEncoded.get(col, row)
        sb += p.toChar
      }
      sb += '\n'
    }
    sb.dropRight(1).toString
  }

  case class Palette(values: Array[Char], nodata: Char = '∘') {
    def colorRamp: ColorRamp = ColorRamp(values.map(_.toInt))
    def length: Int = values.length
  }

  object Palette {
    val WIDE = Palette(" .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$")
    val NARROW = Palette( " .:-=+*#%@")
    val HATCHING = Palette("    ...,,;;---===+++xxxXX##")
    val FILLED = Palette(" ▤▦▩█")
    val STIPLED = Palette(" ▖▚▜█")
    val BINARY = Palette(" ■")

    def apply(str: String): Palette = apply(str.toCharArray)
  }
}
