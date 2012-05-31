package geotrellis.data

import scala.math.{min, max}
import java.io.{File, FileWriter, BufferedReader, BufferedWriter, PrintWriter}

import Console.printf

import geotrellis._
import geotrellis.process._
import geotrellis.util._

final class AsciiReadState(path:String,
                           val layer:RasterLayer,
                           val target:RasterExtent) extends ReadState {
  var ncols:Int = 0
  var nrows:Int = 0
  var xllcorner:Double = 0.0
  var yllcorner:Double = 0.0
  var cellsize:Double = 0.0
  var nodata_value:Int = -9999

  var ints:IntArrayRasterData = null

  def createRasterData(size:Int) = IntArrayRasterData.empty(size)

  def getNoDataValue = nodata_value

  val intRe = """^(-?[0-9]+)$""".r
  val floatRe = """^(-?[0-9]+\.[0-9]+)$""".r

  def getBufferedReader() = {
    val fh = new File(path)
    if (!fh.canRead) throw new Exception("you can't read '%s' so how can i?".format(path))
    val fr = new java.io.FileReader(path)
    new BufferedReader(fr)
  }

  def initSource(pos:Int, size:Int) {
    readMetadata()

    assert (pos == 0)
    assert (size == ncols * nrows)

    val xmin = xllcorner
    val ymin = yllcorner
    val xmax = xllcorner + ncols * cellsize
    val ymax = yllcorner + nrows * cellsize

    ints = IntArrayRasterData.ofDim(size)
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
          if (toks.length != ncols) {
            throw new Exception("saw %d cols, expected %d: %s".format(toks.length, ncols, path))
          }

          var i = 0
          while (i < ncols) {
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
  def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    dest(destIndex) = ints(sourceIndex)
  }

  def readMetadata() {
    val br = getBufferedReader()

    try {
      var done = false
      while (!done) {
        val line = br.readLine().trim()
        val toks = line.split(" ")
  
        if (line == null) throw new Exception("premature end of file: %s".format(path))
        if (toks.length == 0) throw new Exception("illegal empty line: %s".format(path))
  
        if (line.charAt(0).isDigit) {
          done = true
        } else {
          toks match {
            case Array("nrows", intRe(n)) => nrows = n.toInt
            case Array("ncols", intRe(n)) => ncols = n.toInt
            case Array("xllcorner", floatRe(n)) => xllcorner = n.toDouble
            case Array("yllcorner", floatRe(n)) => yllcorner = n.toDouble
            case Array("cellsize", floatRe(n)) => cellsize = n.toDouble
            case Array("nodata_value", intRe(n)) => nodata_value = n.toInt
  
            case _ => throw new Exception("mal-formed line '%s'".format(line))
          }
        }
      }
    } finally {
      br.close()
    }
  }

  def loadRasterExtent() = {
    readMetadata()

    val xmin = xllcorner
    val ymin = yllcorner
    val xmax = xllcorner + ncols * cellsize
    val ymax = yllcorner + nrows * cellsize
    val e = Extent(xmin, ymin, xmax, ymax)

    RasterExtent(e, cellsize, cellsize, ncols, nrows)
  }
}

object AsciiReader extends FileReader {
  def readStateFromPath(path:String, layer:RasterLayer, target:RasterExtent) = {
    new AsciiReadState(path, layer, target)
  }
  def readStateFromCache(b:Array[Byte], layer:RasterLayer, target:RasterExtent) = {
    sys.error("caching ascii grid is not supported")
  }

  override def readMetadata(path:String) = {
    val state = new AsciiReadState(path, null, null)
    val (base, typ) = Filesystem.split(path)
    RasterLayer("", typ, "", base, state.loadRasterExtent(), 3857, 0.0, 0.0)
  }
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

    val data = raster.data

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
