package geotrellis.data

import scala.xml._
import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import geotrellis._
import geotrellis.util._
import geotrellis.process._

final class Arg32ReadState(data:Either[String, Array[Byte]],
                           val layer:RasterLayer,
                           val target:RasterExtent) extends ReadState {
  private var src:ByteBuffer = null

  def width = 4 // byte width
  def getNoDataValue = NODATA

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = src.getInt(sourceIndex * width)
  }
}

object Arg32Reader extends FileReader {
  //def createReadState(path:String, layer:RasterLayer, target:RasterExtent) = {
  //  new Arg32ReadState(path, layer, target)
  //}
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg32ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg32ReadState(Left(p), rl, re)
}

object Arg32Writer extends Writer {
  def width = 4 // byte width
  def rasterType = "arg32"

  def write(path:String, raster:IntRaster, name:String) {
    val i = path.lastIndexOf(".")
    val base = path.substring(0, i)
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg32", raster)
  }

  private def writeData(path:String, raster:IntRaster) {
    val re = raster.rasterExtent
    val cols = re.cols
    val rows = re.rows
    val buffer = Array.ofDim[Byte](cols * width)

    val bos = new BufferedOutputStream(new FileOutputStream(path))

    var row = 0
    while (row < rows) {
      var col = 0
      while (col < cols) {
        val i = col * width 
        var z = raster.get(col, row)
        var j = 0
        while (j < width) {
          val k = width - j - 1
          val mask = 0xff << 8 * k 
          buffer(i + j) = ((z & mask) >> 8 * k).toByte
          //e.g. for width = 4
          //buffer(i)     = ((z & 0xff000000) >> 24).toByte
          //buffer(i + 1) = ((z & 0x00ff0000) >> 16).toByte
          //buffer(i + 2) = ((z & 0x0000ff00) >>  8).toByte
          //buffer(i + 3) = ((z & 0x000000ff)).toByte
          j += 1
        }
        col += 1
      }
      bos.write(buffer)
      row += 1
    }

    bos.close()
  }
}
