/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.util

import geotrellis.util.ByteReader
import java.nio.{ByteBuffer, ByteOrder}

import spire.syntax.cfor._

trait ByteReaderExtensions {

  implicit class ByteReaderUtilities(byteReader: ByteReader) {

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
      byteReader.getChar.toInt

    final def getByteArray(length: Long): Array[Short] = {
      val arr = Array.ofDim[Short](length.toInt)

      cfor(0)( _ < length, _ + 1) { i =>
        arr(i) = ub2s(byteReader.get)
      }

      arr
    }

    final def getByteArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Short] = {
      val arr = Array.ofDim[Short](length.toInt)

      if (length <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)( _ < length, _ + 1) { i =>
          arr(i) = ub2s(bb.get)
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = ub2s(byteReader.get)
        }

        byteReader.position(oldPos)
      }

      arr
    }

    final def getShortArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Int] = {
      val arr = Array.ofDim[Int](length.toInt)

      if (length * 2 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = us2i(bb.getShort)
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = us2i(byteReader.getShort)
        }

        byteReader.position(oldPos)
      }

      arr
    }

    /** Get these as Longs, since they are unsigned and we might want to deal with values greater than Int.MaxValue */
    final def getIntArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Long] = {
      val arr = Array.ofDim[Long](length.toInt)

      if (length * 4 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = ui2l(bb.getInt)
        }
      } else {
        val oldPos = byteReader.position

        byteReader.position(offset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = ui2l(byteReader.getInt)
        }

        byteReader.position(oldPos)
      }

      arr
    }

    final def getLongArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Long] = {
      val arr = Array.ofDim[Long](length.toInt)

      if (length * 8 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getLong
        }
      } else {
        val oldPos = byteReader.position

        byteReader.position(offset)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteReader.getLong
        }

        byteReader.position(oldPos)
      }

      arr
    }

    final def getString(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): String = {
      val sb = new StringBuilder
      if (length <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          sb.append(bb.get.toChar)
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          sb.append(byteReader.get.toChar)
        }

        byteReader.position(oldPos)
      }

      sb.toString
    }

    final def getFractionalArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[(Long, Long)] = {
      val arr = Array.ofDim[(Long, Long)](length.toInt)

      if (length * 8 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = (ui2l(bb.getInt), ui2l(bb.getInt))
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = (ui2l(byteReader.getInt), ui2l(byteReader.getInt))
        }

        byteReader.position(oldPos)
      }

      arr
    }

    /** NOTE: We don't support lengths greater than Int.MaxValue yet (or ever). */
    final def getSignedByteArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Byte] = {
      val len = length.toInt
      if (length <= ttos.size) {
        val arr = Array.ofDim[Byte](len)
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < len, _ + 1) { i =>
          arr(i) = bb.get
        }
        arr
      } else {
        val oldPosition = byteReader.position
        byteReader.position(offset)
        val arr = byteReader.getBytes(len)
        byteReader.position(oldPosition)
        arr
      }
    }

    final def getSignedShortArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Short] = {
      val arr = Array.ofDim[Short](length.toInt)

      if (length * 2 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getShort
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteReader.getShort
        }

        byteReader.position(oldPos)
      }

      arr
    }

    final def getSignedIntArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Int] = {
      val arr = Array.ofDim[Int](1)

      if (length * 8 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getInt
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteReader.getInt
        }

        byteReader.position(oldPos)
      }

      arr
    }

    final def getSignedFractionalArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[(Int, Int)] = {
      val arr = Array.ofDim[(Int, Int)](length.toInt)

      if(length * 8 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)
        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = (bb.getInt, bb.getInt)
        }
      } else {

        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = (byteReader.getInt, byteReader.getInt)
        }

        byteReader.position(oldPos)
      }

      arr
    }

    final def getFloatArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Float] = {
      val arr = Array.ofDim[Float](length.toInt)

      if (length * 4 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getFloat
        }
      } else {
        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteReader.getFloat
        }

        byteReader.position(oldPos)
      }
      arr
    }

    final def getDoubleArray(offset: Long, length: Long)(implicit ttos: TiffTagOffsetSize): Array[Double] = {
      val arr = Array.ofDim[Double](length.toInt)

      if (length * 8 <= ttos.size) {
        val bb = ttos.allocateByteBuffer(offset, byteReader.order)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = bb.getDouble
        }
      } else {

        val oldPos = byteReader.position
        byteReader.position(offset)

        cfor(0)(_ < length, _ + 1) { i =>
          arr(i) = byteReader.getDouble
        }

        byteReader.position(oldPos)
      }

      arr
    }
  }
}
