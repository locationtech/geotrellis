package geotrellis.data

import java.nio.ByteBuffer
import geotrellis._
import geotrellis.util._
import geotrellis.process._
import geotrellis.util.Filesystem

abstract class ArgFloatNReadState(data:Either[String, Array[Byte]],
                                  val rasterExtent:RasterExtent,
                                  val target:RasterExtent,
                                  typ:RasterType) extends ReadState {
  def getType = typ

  final val width:Int = typ.bits / 8

  protected[this] var src:ByteBuffer = null

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }
}

class Float64ReadState(data:Either[String, Array[Byte]],
                       rasterExtent:RasterExtent,
                       target:RasterExtent)
extends ArgFloatNReadState(data, rasterExtent, target, TypeDouble) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, src.getDouble(sourceIndex * width))
  }
}

class Float32ReadState(data:Either[String, Array[Byte]],
                       rasterExtent:RasterExtent,
                       target:RasterExtent)
extends ArgFloatNReadState(data, rasterExtent, target, TypeFloat) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, src.getFloat(sourceIndex * width))
  }
}
