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
import monocle.syntax._

import java.nio.ByteBuffer
import spire.syntax.cfor._

object HorizontalPredictor {
  def apply(tiffTags: TiffTags): Predictor = {
    val colsPerRow = tiffTags.rowSize
    val rowsInSegment: (Int => Int) = { i => tiffTags.rowsInSegment(i) }

    val bandType = tiffTags.bandType

    val predictor =
      if(tiffTags.hasPixelInterleave) {
        new HorizontalPredictor(colsPerRow, rowsInSegment, tiffTags.bandCount)
      } else {
        new HorizontalPredictor(colsPerRow, rowsInSegment, 1)
      }

    predictor.forBandType(bandType)
  }

  private class HorizontalPredictor(cols: Int, rowsInSegment: Int => Int, bandCount: Int) {
    def forBandType(bandType: BandType): Predictor = {
      val applyFunc: (Array[Byte], Int) => Array[Byte] =
        bandType.bitsPerSample match {
          case  8 => apply8 _
          case 16 => apply16 _
          case 32 => apply32 _
          case _ =>
            throw new MalformedGeoTiffException(s"""Horizontal differencing "Predictor" not supported with ${bandType.bitsPerSample} bits per sample""")
        }

      new Predictor {
        val code = Predictor.PREDICTOR_HORIZONTAL
        val checkEndian = true
        def apply(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
          applyFunc(bytes, segmentIndex)
      }
    }

    def apply8(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
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

    def apply16(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
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

    def apply32(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
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
