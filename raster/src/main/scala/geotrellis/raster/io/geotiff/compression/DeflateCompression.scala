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

import geotrellis.raster.io.geotiff.tags.codes.CompressionType._

import java.util.zip.{Inflater, Deflater}

/** Compression level: 0 - 9lvl, default is -1, see [[Deflater]] docs for more information */
case class DeflateCompression(level: Int = Deflater.DEFAULT_COMPRESSION) extends Compression {
  def createCompressor(segmentCount: Int): Compressor =
    new Compressor {
      private val segmentSizes = Array.ofDim[Int](segmentCount)
      def compress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
        segmentSizes(segmentIndex) = segment.size

        val deflater = new Deflater(level)
        // take into account extra 10 leading bytes header, in case of 0 compression level it is important
        val tmp = Array.ofDim[Byte](segment.length + 10)
        deflater.setInput(segment, 0, segment.length)
        deflater.finish()
        val compressedSize = deflater.deflate(tmp)
        val result = Array.ofDim[Byte](compressedSize)
        System.arraycopy(tmp, 0, result, 0, compressedSize)
        result
      }

      def createDecompressor(): Decompressor =
        new DeflateDecompressor(segmentSizes)
    }

  def createDecompressor(segmentSizes: Array[Int]): DeflateDecompressor =
    new DeflateDecompressor(segmentSizes)
}

object DeflateCompression extends DeflateCompression(Deflater.DEFAULT_COMPRESSION)

class DeflateDecompressor(segmentSizes: Array[Int]) extends Decompressor {
  def code = ZLibCoded

  def compress(segment: Array[Byte], level: Int = Deflater.DEFAULT_COMPRESSION): Array[Byte] = {
    val deflater = new Deflater(level)
    // take into account extra 10 leading bytes header, in case of 0 compression level it is important
    val tmp = Array.ofDim[Byte](segment.length + 10)
    deflater.setInput(segment, 0, segment.length)
    val compressedDataLength = deflater.deflate(tmp)
    java.util.Arrays.copyOf(tmp, compressedDataLength)
  }

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val inflater = new Inflater()
    inflater.setInput(segment, 0, segment.length)

    val resultSize = segmentSizes(segmentIndex)
    val result = new Array[Byte](resultSize)
    inflater.inflate(result)
    inflater.reset()
    result
  }
}
