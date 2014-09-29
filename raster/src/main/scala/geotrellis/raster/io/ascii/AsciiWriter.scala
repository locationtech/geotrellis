package geotrellis.raster.io

import geotrellis.raster._
import geotrellis.vector.Extent

import java.io._

import java.util.Locale

object AsciiWriter {
  def cellType = "ascii"
  def dataType = ""

  def write(path: String, raster: Tile, extent: Extent, name: String) {
    write(path, raster, extent, name, NODATA)
  }

  def write(path: String, raster: Tile, extent: Extent, name: String, noData: Int) {
    val g = RasterExtent(extent, raster.cols, raster.rows)
    val e = extent

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
