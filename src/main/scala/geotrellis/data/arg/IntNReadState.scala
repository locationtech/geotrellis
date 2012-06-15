package geotrellis.data.arg

import java.nio.ByteBuffer

import geotrellis._
import geotrellis.data._
import geotrellis.util._
import geotrellis.process._

abstract class IntNReadState(data:Either[String, Array[Byte]],
                             val layer:RasterLayer,
                             val target:RasterExtent,
                             typ:RasterType) extends IntReadState {
  def getType = typ

  final val width:Int = typ.bits / 8

  protected[this] var src:ByteBuffer = null

  /**
   * Returns this datatype's no data value. For signed integers, this is the
   * minimum value storable at this particular bitwidth. For instance, for
   * 8-bit integers (width 1), the value -128 is the no data value.
   */
  def getNoDataValue:Int = -(1 << (width * 8 - 1))

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }
}

class Int8ReadState(data:Either[String, Array[Byte]],
                    layer:RasterLayer,
                    target:RasterExtent)
extends IntNReadState(data, layer, target, TypeByte) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex)
  }
}

class Int16ReadState(data:Either[String, Array[Byte]],
                     layer:RasterLayer,
                     target:RasterExtent)
extends IntNReadState(data, layer, target, TypeShort) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = src.getShort(sourceIndex * 2)
  }
}

class Int32ReadState(data:Either[String, Array[Byte]],
                     layer:RasterLayer,
                     target:RasterExtent)
extends IntNReadState(data, layer, target, TypeInt) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = src.getInt(sourceIndex * 4)
  }
}
