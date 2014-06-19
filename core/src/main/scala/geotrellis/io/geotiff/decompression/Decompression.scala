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
import geotrellis.io.geotiff.decompression.HuffmanDecompression._
import geotrellis.io.geotiff.decompression.PackBitsDecompression._

object Decompression {

  implicit class Decompression(directory: ImageDirectory) {

    def uncompress(bytes: Vector[Byte]): ImageDirectory =
      directory.basicTags.compression match {
        case 1 => directory.copy(imageBytes = Some(bytes))
        case 2 => directory.copy(imageBytes =
          Some(bytes.uncompressHuffman(directory)))
        case 32773 => directory.copy(imageBytes =
          Some(bytes.uncompressPackBits(directory)))
      }

  }

}
