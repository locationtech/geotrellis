package trellis.data

import scala.xml._
import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import trellis._
import trellis.util._
import trellis.constant._
import trellis.raster.IntRaster
import trellis.process._


final class ArgReadState(val path:String,
                         val layer:RasterLayer,
                         val target:RasterExtent) extends ReadState {
  private var src:ByteBuffer = null

  def getNoDataValue = 0

  def initSource(pos:Int, size:Int) {
    src = Filesystem.slurpToBuffer(path, pos, size)
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex)
  }
}

object ArgReader extends FileReader {
  def createReadState(path:String, layer:RasterLayer, target:RasterExtent) = {
    new ArgReadState(path, layer, target)
  }
}


object ArgWriter extends Writer {
  def write(path:String, raster:IntRaster, name:String) {
    val i = path.lastIndexOf(".")
    val base = path.substring(0, i)
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg", raster)
  }

  private def writeData(path:String, raster:IntRaster) {
    val cols = raster.rasterExtent.cols
    val rows = raster.rasterExtent.rows
    val buffer = Array.ofDim[Byte](cols)

    val bos = new BufferedOutputStream(new FileOutputStream(path))

    var row = 0
    while (row < rows) {
      var col = 0
      while (col < cols) {
        var z = raster.get(col, row)
        buffer(col) = ((z & 0x000000ff)).toByte
        col += 1
      }
      bos.write(buffer)
      row += 1
    }

    bos.close()
  }
}
