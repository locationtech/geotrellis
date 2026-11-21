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

import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import java.nio.ByteOrder

/** Unpacks MSB-first unsigned samples into big-endian byte-aligned samples. */
class PackedUnsignedDecompressor(
  underlying: Decompressor,
  segmentSizes: Array[Int],
  bitsPerSample: Int,
  bytesPerSample: Int,
  sourceByteOrder: ByteOrder
) extends Decompressor {
  def code: Int = underlying.code

  def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val packed = underlying.decompress(bytes, segmentIndex)
    if (packed.length != segmentSizes(segmentIndex))
      throw new MalformedGeoTiffException(s"Invalid packed TIFF segment $segmentIndex length")

    val sampleCount = (packed.length.toLong * 8 / bitsPerSample).toInt
    val result = new Array[Byte](Math.multiplyExact(sampleCount, bytesPerSample))
    val mask = (1L << bitsPerSample) - 1
    var accumulator = 0L
    var bits = 0
    var input = 0
    var output = 0
    while (output < result.length) {
      while (bits < bitsPerSample) {
        accumulator = (accumulator << 8) | (packed(input) & 0xffL)
        input += 1
        bits += 8
      }
      bits -= bitsPerSample
      val unpacked = (accumulator >>> bits) & mask
      // libtiff swaps 24-bit samples according to the TIFF byte order; GDAL
      // compensates for that when packing, unlike for other nonstandard widths.
      val value =
        if (bitsPerSample == 24 && sourceByteOrder == ByteOrder.BIG_ENDIAN)
          ((unpacked & 0xffL) << 16) | (unpacked & 0xff00L) | (unpacked >>> 16)
        else unpacked
      var byte = bytesPerSample - 1
      while (byte >= 0) {
        result(output + byte) = (value >>> (8 * (bytesPerSample - 1 - byte))).toByte
        byte -= 1
      }
      output += bytesPerSample
    }
    result
  }
}
