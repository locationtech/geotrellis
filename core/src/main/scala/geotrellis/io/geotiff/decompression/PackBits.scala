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

import geotrellis.io.geotiff._

object PackBitsDecompression {

  implicit class PackBits(bytes: Vector[Byte]) {

    def uncompressPackBits(directory: ImageDirectory): Vector[Byte] = {
      val size =
        directory.basicTags.imageWidth.get * directory.basicTags.imageLength.get
      var buf = Vector.empty[Byte]

      var i = 0
      while (buf.size != size) {
        val n = bytes(i)
        if (n <= 127) {
          buf = buf ++ bytes.drop(i).take(n)
        } else if (n > -128) {
          buf = buf ++ (for (i <- 0 until -n) yield (bytes(i)))
        }

        i += 1
      }

      buf.toVector
    }

  }

}
