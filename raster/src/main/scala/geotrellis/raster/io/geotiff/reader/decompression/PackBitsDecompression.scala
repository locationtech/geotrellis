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

import monocle.syntax._

import spire.syntax.cfor._

object PackBitsDecompression {

  implicit class PackBits(matrix: Array[Array[Byte]]) {

    def uncompressPackBits(implicit directory: ImageDirectory): Array[Array[Byte]] = {
      val len = matrix.length
      val arr = Array.ofDim[Array[Byte]](len)
      cfor(0)(_ < len, _ + 1) { i =>
        val segment = matrix(i)
        val size = directory.imageSegmentByteSize(Some(i)).toInt

        val rowArray = new Array[Byte](size)

        var j = 0
        var total = 0
        while (total != size) {
          if (j >= segment.length)
            throw new MalformedGeoTiffException("bad packbits decompression")

          val n = segment(j)
          j += 1
          if (n >= 0 && n <= 127) {
            cfor(0)(_ <= n, _ + 1) { k =>
              rowArray(total + k) = segment(j + k)
            }
            j += n + 1
            total += n + 1
          } else if (n > -128 && n < 0) {
            val b = segment(i)
            j += 1
            cfor(0)(_ >= -n, _ - 1) { k =>
              rowArray(total + k) = b
            }

            total += -n + 1
          }
        }

        arr(i) = rowArray
      }
      arr
    }
  }
}
