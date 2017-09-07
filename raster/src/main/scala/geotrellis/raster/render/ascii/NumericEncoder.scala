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

import java.util.Locale

import geotrellis.raster.{Tile, isNoData}
import spire.syntax.cfor.cfor

import scala.collection.mutable.ArrayBuffer
import scala.math.BigDecimal

/**
 * Functions for converting a tile into an ASCII string matrix of numbers.
 * @since 9/6/17
 */
case object NumericEncoder {
  /**
   * Return ascii art of this raster.
   */
  def integralEncode(tile: Tile): String = {
    val buff = ArrayBuffer[String]()
    var max = 0
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val v = tile.get(col, row)
        val s = if (isNoData(v)) "ND" else s"$v"
        max = math.max(max, s.size)
        buff += s
      }
    }

    formatTileString(buff, max, tile.cols, tile.rows)
  }

  /**
   * Return ascii art of this raster. The single int parameter
   * indicates the number of significant digits to be printed.
   */
  def decimalEncode(tile: Tile, significantDigits: Int = Int.MaxValue): String = {
    val buff = ArrayBuffer[String]()
    val mc = new java.math.MathContext(significantDigits)
    var max = 0
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val v = tile.getDouble(col, row)
        val s = if (isNoData(v)) "ND" else {
          val s = s"$v"
          if (s.length > significantDigits) BigDecimal(s).round(mc).toString
          else s
        }

        max = math.max(s.length, max)
        buff += s
      }
    }

    formatTileString(buff, max, tile.cols, tile.rows)
  }

  /** Helper. */
  private def formatTileString(buff: ArrayBuffer[String], cols: Int, rows: Int, maxSize: Int) = {
    val sb = new StringBuilder
    val limit = math.max(6, maxSize)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val s = buff(row * cols + col)
        val pad = " " * math.max(limit - s.length, 1)
        sb.append(s"$pad$s")
      }

      sb += '\n'
    }
    sb.toString
  }

  /**
   * Returns an ASCII string representation of a subset of the tile, with cell
   * values encoded as hexadecimal integers.
   */
  def rangeEncode(tile: Tile, colMin: Int, colMax: Int, rowMin: Int, rowMax: Int): String = {
    var s = ""
    for (row <- rowMin to rowMax) {
      for (col <- colMin to colMax) {
        val z = tile.get(row, col)
        if (isNoData(z)) {
          s += ".."
        } else {
          s += "%02X".formatLocal(Locale.ENGLISH, z)
        }
      }
      s += "\n"
    }
    s
  }
}
