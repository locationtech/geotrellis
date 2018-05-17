/*
 * Copyright 2018 Azavea
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


import geotrellis.raster.{Tile, isNoData}

import spire.syntax.cfor._

import scala.collection.mutable.ArrayBuffer
import scala.math.BigDecimal

import java.util.Locale


case object NumericEncoder {
  def encodeIntegrals(tile: Tile): String = {
    val cols = tile.cols
    val rows = tile.rows
    val buff = ArrayBuffer[String]()
    var max = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = tile.get(col, row)
        val s = if (isNoData(v)) "ND" else s"$v"
        max = math.max(max, s.size)
        buff += s
      }
    }

    createAsciiTileString(buff.toArray, max, cols, rows)
  }

  def encodeDecimals(tile: Tile, significantDigits: Int = Int.MaxValue): String = {
    val cols = tile.cols
    val rows = tile.rows
    val buff = ArrayBuffer[String]()
    val mc = new java.math.MathContext(significantDigits)
    var max = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = tile.getDouble(col, row)
        val s = if (isNoData(v)) "ND" else {
          val s = s"$v"
          if (s.size > significantDigits) BigDecimal(s).round(mc).toString
          else s
        }

        max = math.max(s.size, max)
        buff += s
      }
    }

    createAsciiTileString(buff.toArray, max, cols, rows)
  }

  def encodeRange(tile: Tile, colMin: Int, colMax: Int, rowMin: Int, rowMax: Int): String = {
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

  private def createAsciiTileString(
    buff: Array[String],
    maxSize: Int,
    cols: Int,
    rows: Int
  ) = {
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
    sb += '\n'
    sb.toString
  }
}
