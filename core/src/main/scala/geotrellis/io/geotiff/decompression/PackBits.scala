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

import geotrellis.io.geotiff._
import geotrellis.io.geotiff.ImageDirectoryLenses._

object PackBitsDecompression {

  implicit class PackBits(matrix: Vector[Vector[Byte]]) {

    def uncompressPackBits(directory: ImageDirectory): Vector[Byte] = {

      def uncompressPackBitsBytes(row: Vector[Byte]) = {

        val imageRowByteSize = directory.imageRowBitsSize / 8

        val array = new Array[Byte](imageRowByteSize.toInt)

        var i = 0
        var total = 0
        while (total != imageRowByteSize) {
          if (i >= row.length)
            throw new MalformedGeoTiffException("bad packbits decompression")

          val n = row(i)
          i += 1
          if (n >= 0 && n <= 127) {
            for (j <- 0 to n) array(total + j) = row(i + j)
            i += n + 1
            total += n + 1
          } else if (n > -128 && n < 0) {
            val b = row(i)
            i += 1
            for (j <- 0 to -n) array(total + j) = b
            total += -n + 1
          }
        }

        array.toVector
      }

      matrix.par.map(uncompressPackBitsBytes(_)).flatten.toVector
    }

  }

}
