package geotrellis.data

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import geotrellis._
import geotrellis.util._
import geotrellis.process._

import scala.math.pow
import scala.xml._

object ArgFormat {
  def noData(width:Int):Int = -(1 << (width * 8 - 1))
}

abstract class ArgNReadState(data:Either[String, Array[Byte]],
                           val layer:RasterLayer,
                           val target:RasterExtent) extends ReadState {
  def width:Int // byte width, e.g. 4 for arg32 with 32bit values

  protected[this] var src:ByteBuffer = null

  // NoData value is the minimum value storable w/ this bitwidth (-1 for sign)
  def getNoDataValue:Int = ArgFormat.noData(width)

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }

  }


trait ArgNWriter extends Writer {
  def width:Int
  def rasterType:String

  def dataType = "Int" + (width * 8).toString

  def write(path:String, raster:IntRaster, name:String) {
    val i = path.lastIndexOf(".")
    val base = path.substring(0, i)
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg", raster)
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
          buffer(i + j) = (z >> 8 * k).toByte

          //e.g. for width = 4
          //buffer(i)     = (z >> 24).toByte
          //buffer(i + 1) = (z >> 16).toByte
          //buffer(i + 2) = (z >> 8).toByte
          //buffer(i + 3) = (z >> 0).toByte
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

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = src.getInt(sourceIndex * width)
  }

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

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex * width)
  }

}


object Arg8Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg8ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg8ReadState(Left(p), rl, re)
}

object Arg8Writer extends ArgNWriter {
  def width = 1
  def rasterType = "arg8"
}
