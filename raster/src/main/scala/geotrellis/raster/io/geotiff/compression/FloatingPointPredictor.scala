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
import monocle.syntax._
import spire.syntax.cfor._

/** See TIFF Technical Note 3 */
object FloatingPointPredictor {
  def apply(tiffTags: TiffTags): Predictor = {
    val colsPerRow = tiffTags.rowSize
    val rowsInSegment: (Int => Int) = { i => tiffTags.rowsInSegment(i) }

    val bandType = tiffTags.bandType

    if(tiffTags.hasPixelInterleave) {
      new FloatingPointPredictor(colsPerRow, rowsInSegment, bandType, tiffTags.bandCount)
    } else {
      new FloatingPointPredictor(colsPerRow, rowsInSegment, bandType, 1)
    }
  }

  private class FloatingPointPredictor(colsPerRow: Int, rowsInSegment: Int => Int, bandType: BandType, bandCount: Int) extends Predictor {
    val code = Predictor.PREDICTOR_FLOATINGPOINT
    val checkEndian = false

    def apply(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
      val rows = rowsInSegment(segmentIndex)
      val stride = bandCount
      val bytesPerSample = bandType.bytesPerSample

      val colValuesPerRow = colsPerRow * bandCount
      val bytesPerRow = colValuesPerRow * bytesPerSample
      cfor(0)(_ < rows, _ + 1) { row =>
        // Undo the byte differencing
        val rowByteIndex = row * bytesPerRow

        val limit = (row + 1) * bytesPerRow
        cfor(rowByteIndex + bandCount)(_ < limit, _ + 1) { i =>
          bytes(i) = (bytes(i) + bytes(i - bandCount)).toByte
        }

        val tmp = Array.ofDim[Byte](bytesPerRow)
        System.arraycopy(bytes, rowByteIndex, tmp, 0, bytesPerRow)

        cfor(0)(_ < colsPerRow * bandCount, _ + 1) { col =>
          cfor(0)(_ < bytesPerSample, _ + 1) { byteIndex =>
            bytes(rowByteIndex + (bytesPerSample * col + byteIndex)) =
              tmp(byteIndex * colsPerRow + col)
          }
        }
      }
      bytes
    }
  }
}
