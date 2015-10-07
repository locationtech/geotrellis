/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.utils

import java.nio.ByteBuffer

import spire.syntax.cfor._

trait ByteBufferExtensions {

  implicit class ByteBufferUtilities(byteBuffer: ByteBuffer) {

    @inline
    final private def ub2s(byte: Byte): Short =
      (byte & 0xFF).toShort

    @inline
    final private def us2i(short: Short): Int =
      short & 0xFFFF

    @inline
    final private def ui2l(int: Int): Long =
      int & 0XFFFFFFFFL

    @inline
    final def getUnsignedShort: Int = 
      byteBuffer.getChar.toInt

    final def getByteArray(length: Int): Array[Short] = {
      val arr = Array.ofDim[Short](length)

      cfor(0)( _ < length, _ + 1) { i =>
        arr(i) = ub2s(byteBuffer.get)
      }

      arr
    }

    final def getByteArray(length: Int, valueOffset: Int): Array[Short] = {
      val arr = Array.ofDim[Short](length)

      if (length <= 4) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset)
        cfor(0)( _ < length, _ + 1) { i =>
          arr(i) = ub2s(bb.get)
        }
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(valueOffset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = ub2s(byteBuffer.get)
        }

        byteBuffer.position(oldPos)
      }

      arr
    }


    final def getShortArray(length: Int, valueOffset: Int): Array[Int] = {
      val arr = Array.ofDim[Int](length)

      if (length <= 2) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = us2i(bb.getShort)
        }
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(valueOffset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = us2i(byteBuffer.getShort)
        }

        byteBuffer.position(oldPos)
      }

      arr
    }

    final def getIntArray(length: Int, valueOffset: Int): Array[Long] = {
      val arr = Array.ofDim[Long](length)

      if (length == 1) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset)
        arr(0) = ui2l(bb.getInt)
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = ui2l(byteBuffer.getInt)
        }

        byteBuffer.position(oldPos)
      }

      arr
    }

    final def getString(length: Int, offset: Int): String = {
      val sb = new StringBuilder
      if (length <= 4) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, offset)
        cfor(0)( _ < length, _ + 1) { i =>
          sb.append(bb.get.toChar)
        }
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          sb.append(byteBuffer.get.toChar)
        }

        byteBuffer.position(oldPos)
      }

      sb.toString
    }

    final def getFractionalArray(length: Int, offset: Int): Array[(Long, Long)] = {
      val arr = Array.ofDim[(Long, Long)](length)

      val oldPos = byteBuffer.position
      byteBuffer.position(offset)

      cfor(0)(_ < length, _ + 1) { i =>
        arr(i) = (ui2l(byteBuffer.getInt), ui2l(byteBuffer.getInt))
      }

      byteBuffer.position(oldPos)

      arr
    }

    final def getSignedByteArray(length: Int, valueOffset: Int): Array[Byte] = {
      val arr = Array.ofDim[Byte](length)

      if (length <= 4) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.get
        }
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(valueOffset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteBuffer.get
        }

        byteBuffer.position(oldPos)
      }

      arr
    }

    final def getSignedByteArray(length: Int): Array[Byte] = {
      val arr = Array.ofDim[Byte](length)
      cfor(0)(_ < length, _ + 1) { i =>
        arr(i) = byteBuffer.get
      }
      arr
    }

    final def getSignedShortArray(length: Int, valueOffset: Int): Array[Short] = {
      val arr = Array.ofDim[Short](length)

      if (length <= 2) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getShort
        }
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(valueOffset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteBuffer.getShort
        }

        byteBuffer.position(oldPos)
      }

      arr
    }

    final def getSignedIntArray(length: Int, valueOffset: Int): Array[Int] = {
      val arr = Array.ofDim[Int](1)

      if (length == 1) {
        arr(0) = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset).getInt
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(valueOffset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteBuffer.getInt
        }

        byteBuffer.position(oldPos)
      }

      arr
    }

    final def getSignedFractionalArray(length: Int, offset: Int): Array[(Int, Int)] = {
      val arr = Array.ofDim[(Int, Int)](length)

      val oldPos = byteBuffer.position
      byteBuffer.position(offset)

      cfor(0)(_ < length, _ + 1) { i =>
        arr(i) = (byteBuffer.getInt, byteBuffer.getInt)
      }

      byteBuffer.position(oldPos)

      arr
    }

    final def getFloatArray(length: Int, valueOffset: Int): Array[Float] = {
      val arr = Array.ofDim[Float](length)

      if (length <= 1) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).putInt(0, valueOffset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getFloat
        }
      } else {
        val oldPos = byteBuffer.position
        byteBuffer.position(valueOffset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteBuffer.getFloat
        }

        byteBuffer.position(oldPos)
      }
      arr
    }

    final def getDoubleArray(length: Int, offset: Int): Array[Double] = {
      val arr = Array.ofDim[Double](length)

      val oldPos = byteBuffer.position
      byteBuffer.position(offset)

      cfor(0)(_ < length, _ + 1) { i =>
        arr(i) = byteBuffer.getDouble
      }

      byteBuffer.position(oldPos)

      arr
    }
  }
}
