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

package geotrellis.io.geotiff.decompression

import monocle.syntax._
import monocle.Macro._

import scala.collection.immutable.HashMap
import scala.util.control.Breaks._

import geotrellis.io.geotiff.utils.LZWBitInputStream
import geotrellis.io.geotiff._
import geotrellis.io.geotiff.ImageDirectoryLenses._

object LZWDecompression {

  implicit class LZW(matrix: Vector[Vector[Byte]]) {

    val tableLimit = 4096

    val limitMap = HashMap[Int, Int](
      9 -> 510,
      10 -> 1022,
      11 -> 2046,
      12 -> 4094
    )

    val ClearCode = 256
    val EoICode = 257

    def uncompressLZW(directory: ImageDirectory): Vector[Vector[Byte]] = {
      val horizontalPredictor = directory |-> predictorLens get match {
        case Some(predictor) if (predictor == 2) => true
        case None | Some(1) => false
      }

      if (horizontalPredictor) {
        val v = directory |-> bitsPerSampleLens get match {
          case Some(vector) => vector
          case None => throw new MalformedGeoTiffException("no bits per sample tag!")
        }

        v foreach {
          e => if (e != 8) throw new MalformedGeoTiffException(
            "bad bits per sample for horizontal prediction in LZW"
          )
        }
      }

      matrix.zipWithIndex.par.map{
        case (segment, i) => uncompressLZWSegment(segment, i, directory,
          horizontalPredictor)
      }.toVector
    }

    private def uncompressLZWSegment(segment: Vector[Byte], segmentIndex: Int,
      directory: ImageDirectory, horizontalPredictor: Boolean) = {

      val bis = LZWBitInputStream(segment)

      val stringTable = Array.ofDim[Array[Byte]](tableLimit)
      var stringTableIndex = 258

      var outputArrayIndex = 0
      val size = directory.imageSegmentByteSize(Some(segmentIndex)).toInt
      val outputArray = Array.ofDim[Byte](size)

      var threshold = 9

      def initializeStringTable = {
        for (i <- 0 until 256) stringTable(i) = Array(i.toByte)
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

      breakable {
        while ( { code = bis.get(threshold); code != EoICode } ) {
          if (code == ClearCode) {
            initializeStringTable
            code = bis.get(threshold)

            if (code == EoICode) break

            writeString(stringTable(code))
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
      }

      if (horizontalPredictor)
        horizontalPredictorConversion(outputArray, directory, segmentIndex)
      else
        outputArray.toVector
    }

    def horizontalPredictorConversion(array: Array[Byte],
      directory: ImageDirectory, index: Int): Vector[Byte] = {
      val width = directory.rowSize
      val height = directory.rowsInSegment(index)

      val samplesPerPixel = directory |-> samplesPerPixelLens get

      for (i <- 0 until height) {
        var count = samplesPerPixel * (i * width + 1)
        for (j <- samplesPerPixel until width * samplesPerPixel) {
          array(count) = (array(count) + array(count - samplesPerPixel)).toByte
          count += 1
        }
      }

      array.toVector
    }

  }

}
