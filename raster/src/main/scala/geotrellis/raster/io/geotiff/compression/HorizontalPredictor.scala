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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException

import java.nio.ByteBuffer
import spire.syntax.cfor._

object HorizontalPredictor {

  def apply(imageData: GeoTiffImageData): Predictor = {
    val colsPerRow = Predictor.colsPerRow(imageData)
    val rowsInSegment = Predictor.rowsInSegment(imageData)

    val bandType = imageData.bandType

    val predictor =
      if (imageData.segmentLayout.hasPixelInterleave)
        new HorizontalPredictor(colsPerRow, rowsInSegment, imageData.bandCount)
      else
        new HorizontalPredictor(colsPerRow, rowsInSegment, 1)

    predictor.forBandType(bandType)
  }


  def apply(tiffTags: TiffTags): Predictor = {
    val colsPerRow = tiffTags.rowSize
    val rowsInSegment: Int => Int = { i => tiffTags.rowsInSegment(i) }

    val bandType = tiffTags.bandType

    val predictor =
      if (tiffTags.hasPixelInterleave) {
        new HorizontalPredictor(colsPerRow, rowsInSegment, tiffTags.bandCount)
      } else {
        new HorizontalPredictor(colsPerRow, rowsInSegment, 1)
      }

    predictor.forBandType(bandType)
  }

  private class HorizontalPredictor(cols: Int, rowsInSegment: Int => Int, bandCount: Int) {
    def forBandType(bandType: BandType): Predictor = {
      val encodeFunc: (Array[Byte], Int) => Array[Byte] =
        bandType.bitsPerSample match {
          case 8 => encode8
          case 16 => encode16
          case 32 => encode32
          case _ =>
            throw new MalformedGeoTiffException(s"""Horizontal differencing "Predictor" not supported with ${bandType.bitsPerSample} bits per sample""")
        }

      val decodeFunc: (Array[Byte], Int) => Array[Byte] =
        bandType.bitsPerSample match {
          case 8 => decode8
          case 16 => decode16
          case 32 => decode32
          case _ =>
            throw new MalformedGeoTiffException(s"""Horizontal differencing "Predictor" not supported with ${bandType.bitsPerSample} bits per sample""")
        }


      new Predictor {
        val code: Int = Predictor.PREDICTOR_HORIZONTAL
        val checkEndian = true

        override def encode(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
          encodeFunc(bytes, segmentIndex)

        override def decode(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
          decodeFunc(bytes, segmentIndex)
      }
    }

    def encode8(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val encodedBytes = new Array[Byte](bytes.length)
      val rows = rowsInSegment(segmentIndex)
      val n = bytes.length / rows

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(n - 1)({ k => k >= 0 }, _ - 1) { col =>
          val index = row * n + col
          if (col < bandCount)
            encodedBytes(index) = bytes(index)
          else
            encodedBytes(index) = (bytes(index) - bytes(index - bandCount)).toByte
        }
      }
      encodedBytes
    }

    def decode8(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val rows = rowsInSegment(segmentIndex)

      cfor(0)(_ < rows, _ + 1) { row =>
        var count = bandCount * (row * cols + 1)
        cfor(bandCount)({ k => k < cols * bandCount && k < bytes.length }, _ + 1) { k =>
          bytes(count) = (bytes(count) + bytes(count - bandCount)).toByte
          count += 1
        }
      }
      bytes
    }

    def encode16(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val encodedBytes = new Array[Byte](bytes.length)
      val encodedBuffer = ByteBuffer.wrap(encodedBytes).asShortBuffer
      val buffer = ByteBuffer.wrap(bytes).asShortBuffer

      val rows = rowsInSegment(segmentIndex)
      val n = bytes.length / (rows * 2)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(n - 1)({ k => k >= 0 }, _ - 1) { col =>
          val index = row * n + col
          if (col < bandCount)
            encodedBuffer.put(index, buffer.get(index))
          else
            encodedBuffer.put(index, (buffer.get(index) - buffer.get(index - bandCount)).toShort)
        }
      }
      encodedBytes
    }

    def decode16(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val buffer = ByteBuffer.wrap(bytes).asShortBuffer
      val rows = rowsInSegment(segmentIndex)

      cfor(0)(_ < rows, _ + 1) { row =>
        var count = bandCount * (row * cols + 1)
        cfor(bandCount)(_ < cols * bandCount, _ + 1) { k =>
          buffer.put(count, (buffer.get(count) + buffer.get(count - bandCount)).toShort)
          count += 1
        }
      }
      bytes
    }

    def encode32(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val encodedBytes = new Array[Byte](bytes.length)
      val encodedBuffer = ByteBuffer.wrap(encodedBytes).asIntBuffer
      val buffer = ByteBuffer.wrap(bytes).asIntBuffer

      val rows = rowsInSegment(segmentIndex)
      val n = bytes.length / (rows * 4)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(n - 1)({ k => k >= 0 }, _ - 1) { col =>
          val index = row * n + col
          if (col < bandCount)
            encodedBuffer.put(index, buffer.get(index))
          else
            encodedBuffer.put(index, buffer.get(index) - buffer.get(index - bandCount))
        }
      }
      encodedBytes
    }

    def decode32(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val buffer = ByteBuffer.wrap(bytes).asIntBuffer
      val rows = rowsInSegment(segmentIndex)

      cfor(0)(_ < rows, _ + 1) { row =>
        var count = bandCount * (row * cols + 1)
        cfor(bandCount)(_ < cols * bandCount, _ + 1) { k =>
          buffer.put(count, buffer.get(count) + buffer.get(count - bandCount))
          count += 1
        }
      }
      bytes
    }
  }
}
