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

import geotrellis.raster.io.geotiff.tags.codes.CompressionType.*

import java.io.ByteArrayOutputStream
import java.util.zip.{Inflater, Deflater}

/** Compression level: 0 - 9lvl, default is -1, see [[Deflater]] docs for more information */
case class DeflateCompression(level: Int = Deflater.DEFAULT_COMPRESSION) extends Compression {
  def createCompressor(segmentCount: Int): Compressor =
    new Compressor {
      private val segmentSizes = Array.ofDim[Int](segmentCount)
      def compress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
        segmentSizes(segmentIndex) = segment.size

        DeflateCompression.compress(segment, level)
      }

      def createDecompressor(): Decompressor =
        new DeflateDecompressor(segmentSizes)
    }

  def createDecompressor(segmentSizes: Array[Int]): DeflateDecompressor =
    new DeflateDecompressor(segmentSizes)
}

object DeflateCompression extends DeflateCompression(Deflater.DEFAULT_COMPRESSION) {
  private[compression] def compress(segment: Array[Byte], level: Int): Array[Byte] = {
    val deflater = new Deflater(level)
    try {
      deflater.setInput(segment)
      deflater.finish()
      val output = new ByteArrayOutputStream()
      val buffer = new Array[Byte](8192)
      while (!deflater.finished()) {
        val size = deflater.deflate(buffer)
        if (size == 0 && !deflater.finished())
          throw new IllegalStateException("Deflater failed to finish the compressed stream")
        output.write(buffer, 0, size)
      }
      output.toByteArray
    } finally deflater.end()
  }
}

class DeflateDecompressor(segmentSizes: Array[Int]) extends Decompressor {
  def code = ZLibCoded

  def compress(segment: Array[Byte], level: Int = Deflater.DEFAULT_COMPRESSION): Array[Byte] =
    DeflateCompression.compress(segment, level)

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val inflater = new Inflater()
    inflater.setInput(segment, 0, segment.length)

    val resultSize = segmentSizes(segmentIndex)
    val result = new Array[Byte](resultSize)
    inflater.inflate(result)
    inflater.reset()
    inflater.end()
    result
  }
}
