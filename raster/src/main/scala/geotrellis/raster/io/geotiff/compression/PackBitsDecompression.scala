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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags.Tags

import monocle.syntax._
import spire.syntax.cfor._

object PackBitsDecompressor {
  def apply(tags: Tags): PackBitsDecompressor = {
    val segmentSize: (Int => Int) = { i => tags.imageSegmentByteSize(i).toInt }

    new PackBitsDecompressor(segmentSize)
  }
}

class PackBitsDecompressor(segmentSize: Int => Int) extends Decompressor {
  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val size = segmentSize(segmentIndex)

    val rowArray = new Array[Byte](size)

    var j = 0
    var total = 0
    val len = segment.length
    while (total != size && j < len) {

      val headerByte = segment(j)
      j += 1

      if (0 <= headerByte) {
        // next (headerByte + 1) values in segment are literal values
        val limit = total + headerByte + 1
        while(total < limit) {
          rowArray(total) = segment(j)
          total += 1
          j += 1
        }
      } else if (-128 < headerByte && headerByte < 0) {
        // The next byte of data repeated (1 - headerByte) times
        val b = segment(j)
        j += 1

        val limit = total + (1 - headerByte)
        while(total < limit) {
          rowArray(total) = b
          total += 1
        }
      }
    }

    rowArray
  }
}
