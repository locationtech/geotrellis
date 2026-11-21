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

import geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import java.util.zip.Inflater

/** Unpacks MSB-first 15-bit samples into big-endian unsigned shorts. */
class UInt15Decompressor(segmentSizes: Array[Int]) extends Decompressor {
  def code: Int = ZLibCoded

  def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val packed = new Array[Byte](segmentSizes(segmentIndex))
    val inflater = new Inflater()
    try {
      inflater.setInput(bytes)
      var offset = 0
      while (offset < packed.length && !inflater.finished() && !inflater.needsInput() && !inflater.needsDictionary()) {
        val count = inflater.inflate(packed, offset, packed.length - offset)
        if (count == 0 && !inflater.finished())
          throw new MalformedGeoTiffException(s"Unable to decompress 15-bit TIFF segment $segmentIndex")
        offset += count
      }
      if (offset != packed.length || !inflater.finished())
        throw new MalformedGeoTiffException(s"Invalid compressed 15-bit TIFF segment $segmentIndex")
    } finally {
      inflater.end()
    }
    val sampleCount = packed.length * 8 / 15
    val result = new Array[Byte](sampleCount * 2)
    var accumulator = 0
    var bits = 0
    var input = 0
    var output = 0
    while (output < result.length) {
      while (bits < 15) {
        accumulator = (accumulator << 8) | (packed(input) & 0xff)
        input += 1
        bits += 8
      }
      bits -= 15
      val value = (accumulator >>> bits) & 0x7fff
      result(output) = (value >>> 8).toByte
      result(output + 1) = value.toByte
      output += 2
    }
    result
  }
}
