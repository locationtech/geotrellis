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

    def getByteVector(length: Int): Vector[Byte] =
      (for (i <- 0 until length) yield byteBuffer.get).toVector

    def getByteVector(length: Int, valueOffset: Int): Vector[Int] =
      if (length <= 4) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(valueOffset)
          (for (i <- 0 until length) yield bb.get.toInt).toVector
      } else {
        val oldOffset = byteBuffer.position
        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          byteBuffer.get.toInt).toVector
        byteBuffer.position(oldOffset)
        arr
      }

    def getShortVector(length: Int, valueOffset: Int): Vector[Int] =
      if (length <= 2) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(valueOffset)
          (for (i <- 0 until length) yield bb.getShort.toInt).toVector
      } else {
        val oldOffset = byteBuffer.position
        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield
          byteBuffer.getShort.toInt).toVector
        byteBuffer.position(oldOffset)
        arr
      }

    def getIntVector(length: Int, valueOffset: Int): Vector[Int] =
      if (length == 1) {
        val bb = ByteBuffer.allocate(4).order(byteBuffer.order).
          putInt(valueOffset)
          (for (i <- 0 until length) yield bb.getInt).toVector
      } else {
        val oldOffset = byteBuffer.position
        byteBuffer.position(valueOffset)
        val arr = (for (i <- 0 until length) yield byteBuffer.getInt).toVector
        byteBuffer.position(oldOffset)
        arr
      }



    def getString(length: Int, offset: Int): String = {
      val oldOffset = byteBuffer.position
      byteBuffer.position(offset)
      val string = (for (i <- 0 until length) yield
        byteBuffer.get.toChar).mkString
      byteBuffer.position(oldOffset)
      string
    }

    def getFractionalVector(length: Int, offset: Int) = {
      val oldOffset = byteBuffer.position
      byteBuffer.position(offset)
      val fractionals = (for (i <- 0 until length) yield (byteBuffer.getInt,
        byteBuffer.getInt)).toVector
      byteBuffer.position(oldOffset)
      fractionals
    }

    def getDoubleVector(length: Int, offset: Int) = {
      val oldOffset = byteBuffer.position
      byteBuffer.position(offset)
      val doubles = (for (i <- 0 until length) yield
        byteBuffer.getDouble).toVector
      byteBuffer.position(oldOffset)
      doubles
    }

    def getShortFromInt(value: Int) = ByteBuffer.allocate(4)
      .order(byteBuffer.order).putInt(value).getShort.toInt

  }

}
