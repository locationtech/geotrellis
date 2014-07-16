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

package geotrellis.data

import scala.math.{min, max}
import java.io.{File, FileWriter, BufferedReader, BufferedWriter, PrintWriter}
import java.util.Locale
import Console.printf
import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.util._
import geotrellis.util.Filesystem

final class AsciiReadState(path:String,
                           val rasterExtent:RasterExtent,
                           val target:RasterExtent,
                           val noDataValue:Int) extends IntReadState {
  private var ints:IntArrayRasterData = null

  def getType = TypeInt

  def getNoDataValue = noDataValue

  val intRe = """^(-?[0-9]+)$""".r
  val floatRe = """^(-?[0-9]+\.[0-9]+)$""".r

  def getBufferedReader() = {
    val fh = new File(path)
    if (!fh.canRead) throw new Exception(s"you can't read '$path' so how can i?")
    val fr = new java.io.FileReader(path)
    new BufferedReader(fr)
  }

  def initSource(pos:Int, size:Int) {
    assert (pos == 0)
    assert (size == rasterExtent.cols * rasterExtent.rows)

    ints = IntArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
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
  def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = ints(sourceIndex)
  }
}

class AsciiReader(path:String, noDataValue:Int) extends FileReader(path) {
  def readStateFromPath(rasterType:RasterType,
                        rasterExtent:RasterExtent,
                        targetExtent:RasterExtent):ReadState = {
    new AsciiReadState(path, rasterExtent, targetExtent, noDataValue)
  }

  def readStateFromCache(b:Array[Byte],
                         rasterType:RasterType,
                         rasterExtent:RasterExtent,
                         targetExtent:RasterExtent) =
    sys.error("caching ascii grid is not supported")
}

object AsciiWriter extends Writer {
  def rasterType = "ascii"
  def dataType = ""

  def write(path:String, raster:Raster, name:String) {
    write(path, raster, name, NODATA)
  }

  def write(path:String, raster:Raster, name:String, noData:Int) {
    val g = raster.rasterExtent
    val e = raster.rasterExtent.extent

    if (g.cellwidth != g.cellheight) throw new Exception("raster cannot be written as ASCII")

    val pw = new PrintWriter(new BufferedWriter(new FileWriter(path)))

    pw.write(s"nrows ${g.rows}\n")
    pw.write(s"ncols ${g.cols}\n")
    pw.write("xllcorner %.12f\n".formatLocal(Locale.ENGLISH, e.xmin))
    pw.write("yllcorner %.12f\n".formatLocal(Locale.ENGLISH, e.ymin))
    pw.write("cellsize %.12f\n".formatLocal(Locale.ENGLISH, g.cellwidth))
    pw.write(s"nodata_value $noData\n")

    val data = raster.toArray

    var y = 0
    while (y < g.rows) {
      val span = y * g.cols

      var x = 0
      while (x < g.cols) {
        pw.print(" ")
        pw.print(data(span + x))
        x += 1
      }
      y += 1
      pw.print("\n")
    }

    pw.close()
  }
}
