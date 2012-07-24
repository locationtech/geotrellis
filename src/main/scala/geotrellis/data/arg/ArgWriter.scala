package geotrellis.data.arg

import java.io.{BufferedOutputStream, DataOutputStream, FileOutputStream}

import geotrellis._
import geotrellis.data._
import geotrellis.util._
import geotrellis.process._

case class ArgWriter(typ:RasterType) extends Writer {
  def width = typ.bits / 8
  def rasterType = typ.name
  def dataType = "arg"

  def write(path:String, raster:Raster, name:String) {
    val base = path.substring(0, path.lastIndexOf("."))
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg", raster)
  }

  private def writeBytes(data:RasterData, cols:Int, rows:Int, dos:DataOutputStream) {
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < cols) {
        dos.writeByte(data.get(col, row))
        col += 1
      }
      row += 1
    }
  }

  private def writeData(path:String, raster:Raster) {
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))
    CellWriter.byType(typ).writeCells(raster, dos)
    dos.close()
  }
}
