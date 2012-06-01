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

  def write(path:String, raster:Raster, name:String) {
    val i = path.lastIndexOf(".")
    val base = path.substring(0, i)
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg", raster)
  }

  private def writeData(path:String, raster:Raster) {
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

// arg int32

class Arg32ReadState(data:Either[String, Array[Byte]],
                     layer:RasterLayer,
                     target:RasterExtent)  extends ArgNReadState(data, layer, target) {
  final val width = 4

  def createRasterData(size:Int) = IntArrayRasterData.empty(size)

  @inline
  def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    dest(destIndex) = src.getInt(sourceIndex * width)
  }
}

object Arg32Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg32ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg32ReadState(Left(p), rl, re)
}

object Arg32Writer extends ArgNWriter {
  final val width = 4
  final val rasterType = "arg32"
}

// arg int16

class Arg16ReadState(data:Either[String, Array[Byte]],
                    layer:RasterLayer,
                    target:RasterExtent) extends ArgNReadState(data,layer,target) {
  final val width = 2

  def createRasterData(size:Int) = ShortArrayRasterData.empty(size)

  @inline
  def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex * width)
  }
}

object Arg16Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg16ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg16ReadState(Left(p), rl, re)
}

object Arg16Writer extends ArgNWriter {
  final val width = 2
  final val rasterType = "arg16"
}

// arg int8

class Arg8ReadState(data:Either[String, Array[Byte]],
                    layer:RasterLayer,
                    target:RasterExtent) extends ArgNReadState(data,layer,target) {
  final val width = 1

  def createRasterData(size:Int) = ByteArrayRasterData.empty(size)

  @inline
  def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex * width)
  }
}


object Arg8Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Arg8ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Arg8ReadState(Left(p), rl, re)
}

object Arg8Writer extends ArgNWriter {
  final val width = 1
  final val rasterType = "arg8"
}

// arg float64

abstract class FloatNReadState(data:Either[String, Array[Byte]],
                               val layer:RasterLayer,
                               val target:RasterExtent) extends ReadState {

  def width:Int

  protected[this] var src:ByteBuffer = null

  def getNoDataValue = sys.error("not used") // REFACTOR: ugh

  override def translate(data:StrictRasterData) = data

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }
}


class Float64ReadState(data:Either[String, Array[Byte]],
                       layer:RasterLayer,
                       target:RasterExtent)  extends FloatNReadState(data, layer, target) {
  final val width = 8

  def createRasterData(size:Int) = DoubleArrayRasterData.empty(size)

  @inline
  def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, src.getDouble(sourceIndex * width))
  }
}

object Float64Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Float64ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Float64ReadState(Left(p), rl, re)
}


class Float32ReadState(data:Either[String, Array[Byte]],
                       layer:RasterLayer,
                       target:RasterExtent)  extends FloatNReadState(data, layer, target) {
  final val width = 4

  def createRasterData(size:Int) = FloatArrayRasterData.empty(size)

  @inline
  def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, src.getFloat(sourceIndex * width))
  }
}

object Float32Reader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new Float32ReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new Float32ReadState(Left(p), rl, re)
}
