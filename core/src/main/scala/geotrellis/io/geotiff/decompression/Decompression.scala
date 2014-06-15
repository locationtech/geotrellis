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
import geotrellis.io.geotiff.decompression.HuffmanObject._
import geotrellis.io.geotiff.decompression.PackBitsObject._

object DecompressionObject {

  implicit class Decompression(tags: IFDTags) {

    def uncompress(strips: Array[Array[Char]]): IFDTags =
      tags.basics.compression match {
        case 1 => tags.copy(imageBytes = Some(strips.flatten))
        case 2 => tags.copy(imageBytes = Some(strips.uncompressHuffman(tags)))
        case 32773 => tags.copy(imageBytes =
          Some(strips.uncompressPackBits(tags)))
      }

  }

  def splitToRows(strips: Array[Array[Char]], tags: IFDTags) = {
    val rowsPerStrip = tags.basics.rowsPerStrip
    val rowLength = tags.basics.imageWidth.get
    val mod = tags.basics.imageLength.get % rowsPerStrip
    (for (i <- 0 until strips.length) yield {
      val strip = strips(i)
      val threshold = if (i == strips.length - 1 && mod != 0) mod else
        rowsPerStrip

      (for (j <- 0 until threshold) yield {
        strip.drop(j * rowLength).take(rowLength)
      }).toArray
    }).flatten.toArray
  }

}
