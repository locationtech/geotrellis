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

package geotrellis.raster.io.ascii

import geotrellis.raster._
import geotrellis.raster.io._

import java.io._

final class AsciiReadState(path: String,
                           val rasterExtent: RasterExtent,
                           val target: RasterExtent,
                           val noDataValue: Int) extends IntReadState {
  private var ints: IntArrayTile = null

  def getType = IntConstantNoDataCellType

  def getNoDataValue = noDataValue

  val intRe = """^(-?[0-9]+)$""".r
  val floatRe = """^(-?[0-9]+\.[0-9]+)$""".r

  def getBufferedReader() = {
    val fh = new File(path)
    if (!fh.canRead) throw new Exception(s"cannot read $path")
    val fr = new java.io.FileReader(path)
    new BufferedReader(fr)
  }

  def initSource(pos: Int, size: Int) {
    assert (pos == 0)
    assert (size == rasterExtent.cols * rasterExtent.rows)

    ints = IntArrayTile.ofDim(rasterExtent.cols, rasterExtent.rows)
    val br = getBufferedReader()
    try {
      var done = false

      var i = 0
      var n = 0
      while (!done) {
        val line = br.readLine()
        if (line == null) throw new Exception(s"premature end of file: $path")
        if (line.length == 0) throw new Exception(s"illegal empty line: $path")

        val toks = line.trim().split(" ")
        if (toks(0).charAt(0).isDigit) {
          if (toks.length != rasterExtent.cols) {
            throw new Exception(s"saw ${toks.length} cols, expected ${rasterExtent.cols}: $path")
          }

          var i = 0
          while (i < rasterExtent.cols) {
            ints(n) = toks(i).toInt
            i += 1
            n += 1
          }
        }
        if (n >= size) done = true
      }

      if (n != size) throw new Exception(s"saw $n rows, expected $size: $path")

    } finally {
      br.close()
    }
  }

  @inline
  def assignFromSource(sourceIndex: Int, dest: MutableArrayTile, destIndex: Int) {
    dest(destIndex) = ints(sourceIndex)
  }
}
