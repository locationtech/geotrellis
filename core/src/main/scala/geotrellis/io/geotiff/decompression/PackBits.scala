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

        var buf = Vector.empty[Byte]

        var i = 0
        while (buf.size != imageRowByteSize) {
          val n = row(i)
          if (n <= 127) {
            i += n
            buf = buf ++ row.drop(i).take(n)
          } else if (n > -128) {
            i += 1
            buf = buf ++ (for (i <- 0 until -n) yield (row(i)))
          }

          i += 1
        }

        buf.toVector
      }

      matrix.map(uncompressPackBitsBytes(_)).flatten.toVector
    }

  }

}
