package geotrellis.data.png

import java.nio.ByteBuffer

import geotrellis._

object Util {
  @inline final def byte(i:Int):Byte = i.toByte
  @inline final def shift(n:Int, i:Int):Byte = byte(n >> i)

  /**
   * ByteBuffer boiler-plate stuff below.
   */
  def initByteBuffer32(bb:ByteBuffer, data:RasterData, size:Int) {
    val d = data.asArray.getOrElse(sys.error("can't get data array")) 
    var j = 0
    while (j < size) {
      val z = d(j)
      bb.put(byte(z >> 24))
      bb.put(byte(z >> 16))
      bb.put(byte(z >> 8))
      bb.put(byte(z))
      j += 1
    }
  }

  def initByteBuffer24(bb:ByteBuffer, data:RasterData, size:Int) {
    val d = data.asArray.getOrElse(sys.error("can't get data array"))
    var j = 0
    while (j < size) {
      val z = d(j)
      bb.put(byte(z >> 16))
      bb.put(byte(z >> 8))
      bb.put(byte(z))
      j += 1
    }
  }

  def initByteBuffer16(bb:ByteBuffer, data:RasterData, size:Int) {
    val d = data.asArray.getOrElse(sys.error("can't get data array"))
    var j = 0
    while (j < size) {
      val z = d(j)
      bb.put(byte(z >> 8))
      bb.put(byte(z))
      j += 1
    }
  }

  def initByteBuffer8(bb:ByteBuffer, data:RasterData, size:Int) {
    val d = data.asArray.getOrElse(sys.error("can't get data array"))
    var j = 0
    while (j < size) {
      bb.put(byte(d(j)))
      j += 1
    }
  }
}
