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

package geotrellis.io.geotiff.utils

import java.nio.ByteBuffer

object ByteBufferUtils {

  implicit class ByteBufferUtilities(byteBuffer: ByteBuffer) {

    private def ub2s(byte: Byte): Short =
      (byte.toInt + (if (byte < 0) 255 else 0)).toShort

    private def us2i(short: Short): Int =
      short.toInt + (if (short < 0) 32767 else 0)

    private def ui2l(int: Int): Long =
      int.toLong + (if (int < 0) 2147483647 else 0)

    def goToNextImageDirectory(current: Int, entries: Int) =
      byteBuffer.position(entries * 12 +
        current + 2).position(byteBuffer.getInt)

    def getUnsignedShort = byteBuffer.getChar.toInt

    def getUnsignedShort(value: Int) = ByteBuffer.allocate(4).
      order(byteBuffer.order).putInt(0, value).getChar.toInt

    def getByteVector(length: Int): Vector[Short] =
      (for (i <- 0 until length) yield ub2s(byteBuffer.get)).toVector

    def getByteVector(length: Int, valueOffset: Int): Vector[Short] =
      if (length <= 4) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield ub2s(bb.get)).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          ub2s(byteBuffer.get)).toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getShortVector(length: Int, valueOffset: Int): Vector[Int] =
      if (length <= 2) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield us2i(bb.getShort)).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          us2i(byteBuffer.getShort)).toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getIntVector(length: Int, valueOffset: Int): Vector[Long] =
      if (length == 1) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield ui2l(bb.getInt)).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield ui2l(byteBuffer.getInt))
          .toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getString(length: Int, offset: Int): String = {
      val oldPos = byteBuffer.position

      byteBuffer.position(offset)
      val string = (for (i <- 0 until length) yield
        byteBuffer.get.toChar).mkString

      byteBuffer.position(oldPos)

      string
    }

    def getFractionalVector(length: Int, offset: Int): Vector[(Long, Long)] = {
      val oldPos = byteBuffer.position

      byteBuffer.position(offset)
      val fractionals = (for (i <- 0 until length) yield
        (ui2l(byteBuffer.getInt), ui2l(byteBuffer.getInt))).toVector

      byteBuffer.position(oldPos)

      fractionals
    }

    def getSignedByteVector(length: Int, valueOffset: Int): Vector[Byte] =
      if (length <= 4) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield bb.get).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          byteBuffer.get).toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getSignedByteVector(length: Int): Vector[Byte] =
      (for (i <- 0 until length) yield (byteBuffer.get)).toVector


    def getSignedShortVector(length: Int, valueOffset: Int): Vector[Short] =
      if (length <= 2) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield bb.getShort).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          byteBuffer.getShort).toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getSignedIntVector(length: Int, valueOffset: Int): Vector[Int] =
      if (length == 1) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield bb.getInt).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield byteBuffer.getInt).toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getSignedFractionalVector(length: Int,
      offset: Int): Vector[(Int, Int)] = {
      val oldPos = byteBuffer.position

      byteBuffer.position(offset)
      val fractionals = (for (i <- 0 until length) yield (byteBuffer.getInt,
        byteBuffer.getInt)).toVector

      byteBuffer.position(oldPos)

      fractionals
    }

    def getFloatVector(length: Int, valueOffset: Int): Vector[Float] =
      if (length <= 2) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(0, valueOffset)
          (for (i <- 0 until length) yield bb.getFloat).toVector
      } else {
        val oldPos = byteBuffer.position

        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          byteBuffer.getFloat).toVector

        byteBuffer.position(oldPos)

        arr
      }

    def getDoubleVector(length: Int, offset: Int) = {
      val oldPos = byteBuffer.position

      byteBuffer.position(offset)
      val doubles = (for (i <- 0 until length) yield
        byteBuffer.getDouble).toVector

      byteBuffer.position(oldPos)

      doubles
    }

  }

}
