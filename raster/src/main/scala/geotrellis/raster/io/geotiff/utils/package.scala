package geotrellis.raster.io.geotiff

import geotrellis.raster._
import java.nio.ByteBuffer

package object utils extends ArrayExtensions
  with ByteBufferExtensions
  with ByteInverter
  with GDALNoDataParser {

  implicit class ShortArrayToByte(val arr: Array[Short]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * TypeShort.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asShortBuffer.put(arr)
      result
    }
  }

  implicit class IntArrayToByte(val arr: Array[Int]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * TypeInt.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asIntBuffer.put(arr)
      result
    }
  }

  implicit class FloatArrayToByte(val arr: Array[Float]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * TypeFloat.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asFloatBuffer.put(arr)
      result
    }
  }

  implicit class DoubleArrayToByte(val arr: Array[Double]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * TypeDouble.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asDoubleBuffer.put(arr)
      result
    }
  }

}
