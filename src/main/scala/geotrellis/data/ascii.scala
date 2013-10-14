package geotrellis.data

import scala.math.{min, max}
import java.io.{File, FileWriter, BufferedReader, BufferedWriter, PrintWriter}
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
    if (!fh.canRead) throw new Exception("you can't read '%s' so how can i?".format(path))
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
        if (line == null) throw new Exception("premature end of file: %s".format(path))
        if (line.length == 0) throw new Exception("illegal empty line: %s".format(path))

        val toks = line.trim().split(" ")
        if (toks(0).charAt(0).isDigit) {
          if (toks.length != rasterExtent.cols) {
            throw new Exception("saw %d cols, expected %d: %s".format(toks.length, rasterExtent.cols, path))
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

      if (n != size) throw new Exception("saw %d rows, expected %d: %s".format(n, size, path))

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

    pw.write("nrows %d\n".format(g.rows))
    pw.write("ncols %d\n".format(g.cols))
    pw.write("xllcorner %.12f\n".format(e.xmin))
    pw.write("yllcorner %.12f\n".format(e.ymin))
    pw.write("cellsize %.12f\n".format(g.cellwidth))
    pw.write("nodata_value %d\n".format(noData))

    val data = raster.data.asArray

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
