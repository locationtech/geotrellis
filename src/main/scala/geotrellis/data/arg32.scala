package geotrellis.data

import scala.xml._
import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import geotrellis._
import geotrellis.util._
import geotrellis.process._

abstract class ArgNReadState(data:Either[String, Array[Byte]],
                           val layer:RasterLayer,
                           val target:RasterExtent) extends ReadState {
  def width:Int // byte width, e.g. 4 for arg32 with 32bit values

  private var src:ByteBuffer = null

  // NoData value is the minimum value storable w/ this bitwidth (-1 for sign)
  def getNoDataValue:Int = 0 - (math.pow(2, width * 8 - 1)).toInt

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


trait ArgNWriter extends Writer {
  def width:Int
  def rasterType:String

  def write(path:String, raster:IntRaster, name:String) {
    val i = path.lastIndexOf(".")
    val base = path.substring(0, i)
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg" + (width * 8).toString(), raster)
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

//REFACTOR: Create a single ArgReader/Writer that passes through bitwidth as an argument.
//          This would require refactoring the Writer/Reader traits.

class Arg32ReadState(data:Either[String, Array[Byte]],
                           layer:RasterLayer,
                           target:RasterExtent)  extends ArgNReadState (data,layer,target) {
  val width = 4
}

object Arg32Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg32ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg32ReadState(Left(p), rl, re)
}

object Arg32Writer extends ArgNWriter {
  def width = 4
  def rasterType = "arg32"
}

class Arg8ReadState(data:Either[String, Array[Byte]],
                    layer:RasterLayer,
                    target:RasterExtent) extends ArgNReadState(data,layer,target) {
  val width = 1
}

class Arg8Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg8ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg8ReadState(Left(p), rl, re)
}

object Arg8Writer extends ArgNWriter {
  def width = 1
  def rasterType = "arg8"
}
