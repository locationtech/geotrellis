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

package geotrellis.raster.io.geotiff.reader.decompression

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.reader.utils.ByteInverterUtils

import monocle.syntax._

import scala.collection.immutable.HashMap

import java.util.BitSet

import spire.syntax.cfor._

object LZWDecompression {
  implicit class LZW(matrix: Array[Array[Byte]]) {
    val tableLimit = 4096

    final val initialStringTable = {
      val arr = Array.ofDim[Array[Byte]](tableLimit)
      cfor(0)(_ < 256, _ + 1) { i =>
        arr(i) = Array(i.toByte)
      }

      arr
    }

    val limitMap = HashMap[Int, Int](
      9 -> 510,
      10 -> 1022,
      11 -> 2046,
      12 -> 4094
    )

    val ClearCode = 256
    val EoICode = 257

    def uncompressLZW(directory: ImageDirectory): Array[Array[Byte]] = {
      val horizontalPredictor = (directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._predictor get) match {
        case Some(2) => true
        case None | Some(1) => false
        case Some(i) =>
          throw new MalformedGeoTiffException(s"predictor tag $i is not valid (require 1 or 2)")
      }

      val samplesPerPixel = (directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._samplesPerPixel get)

      if (horizontalPredictor) {
        val v = (directory &|->
          ImageDirectory._basicTags ^|->
          BasicTags._bitsPerSample get) match {
          case Some(vector) => vector
          case None => throw new MalformedGeoTiffException("no bits per sample tag!")
        }

        v foreach {
          e => if (e != 8) throw new MalformedGeoTiffException(
            "bad bits per sample for horizontal prediction in LZW"
          )
        }
      }

      val len = matrix.length
      val arr = Array.ofDim[Array[Byte]](len)

      cfor(0)(_ < len, _ + 1) { i =>
        val segment = matrix(i)
        val bis = new LZWBitInputStream(segment)
        var stringTable = Array.ofDim[Array[Byte]](tableLimit)
        var stringTableIndex = 258

        var outputArrayIndex = 0
        val size = directory.imageSegmentByteSize(Some(i)).toInt
        val outputArray = Array.ofDim[Byte](size)

        var threshold = 9

        def initializeStringTable = {
          stringTable = initialStringTable.clone
          stringTableIndex = 258
          threshold = 9
        }

        def addString(string: Array[Byte]) = {
          stringTable(stringTableIndex) = string
          stringTableIndex += 1

          if (stringTableIndex == 511) threshold = 10
          if (stringTableIndex == 1023) threshold = 11
          if (stringTableIndex == 2047) threshold = 12
        }

        def isInTable(code: Int) = code < stringTableIndex

        var printed = 0

        def writeString(string: Array[Byte]) = {
          System.arraycopy(
            string,
            0,
            outputArray,
            outputArrayIndex,
            string.length
          )

          outputArrayIndex += string.length
        }

        var code = 0
        var oldCode = 0

        var break = false
        while (!break && { code = bis.get(threshold); code != EoICode } ) {
          if (code == ClearCode) {
            initializeStringTable
            code = bis.get(threshold)

            if (code == EoICode) {
              break = true
            } else {
              writeString(stringTable(code))
            }
          } else if (isInTable(code)) {
            val string = stringTable(code)
            writeString(string)

            addString(stringTable(oldCode) :+ string(0))
          } else {
            val string = stringTable(oldCode) :+ stringTable(oldCode)(0)
            writeString(string)
            addString(string)
          }

          oldCode = code
        }

        if (horizontalPredictor) {
          // Convert to horizontal predictor
          val width = directory.rowSize
          val height = directory.rowsInSegment(i)

          cfor(0)(_ < height, _ + 1) { j =>
            var count = samplesPerPixel * (j * width + 1)
            cfor(samplesPerPixel)(_ < width * samplesPerPixel, _ + 1) { k =>
              outputArray(count) = (outputArray(count) + outputArray(count - samplesPerPixel)).toByte
              count += 1
            }
          }
        }

        arr(i) = outputArray
      }

      arr
    }
  }

  /** This class modifies the array passed in */
  private class LZWBitInputStream(arr: Array[Byte]) {

    val len = arr.length

    private var index = 0

    def get(next: Int): Int = {
      if (next + index > len * 8)
        throw new IndexOutOfBoundsException(
          s"Index out of bounds for BitInputStream for LZW decompression."
        )

      val start = index / 8
      val end = (index + next - 1) / 8
      val ebi = (index + next - 1) % 8

      val res =
        if (end - start == 2) {
          val c = ((arr(start) & 0xff) << 16) | ((arr(end - 1) & 0xff) << 8) | (arr(end) & 0xff)
          (c >> (7 - ebi)) & (0xffffff >> (24 - next))
        } else {
          val c = ((arr(start) & 0xff) << 8) | (arr(end) & 0xff)
          (c >> (7 - ebi)) & (0xffff >> (16 - next))
        }

      index += next

      res
    }

    def addToIndex(add: Int) = index += add

    def reset = index = 0

    def getIndex: Int = index

  }

}
