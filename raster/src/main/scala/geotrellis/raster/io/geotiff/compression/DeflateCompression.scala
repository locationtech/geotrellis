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
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException

import java.io.ByteArrayOutputStream
import java.util.zip.{DataFormatException, Inflater, Deflater}

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

class DeflateDecompressor(segmentSizes: Array[Int], strict: Boolean) extends Decompressor {
  def this(segmentSizes: Array[Int]) = this(segmentSizes, false)

  def code = ZLibCoded

  def compress(segment: Array[Byte], level: Int = Deflater.DEFAULT_COMPRESSION): Array[Byte] =
    DeflateCompression.compress(segment, level)

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val inflater = new Inflater()
    try {
      inflater.setInput(segment, 0, segment.length)
      val result = new Array[Byte](segmentSizes(segmentIndex))
      if (!strict) inflater.inflate(result)
      else {
        var offset = 0
        val extra = new Array[Byte](1)
        while (!inflater.finished()) {
          val count =
            if (offset < result.length) inflater.inflate(result, offset, result.length - offset)
            else inflater.inflate(extra)
          if ((offset == result.length && count != 0) || (count == 0 && !inflater.finished()))
            throw new MalformedGeoTiffException(s"Invalid compressed TIFF segment $segmentIndex")
          offset += count
        }
        if (offset != result.length)
          throw new MalformedGeoTiffException(s"Invalid compressed TIFF segment $segmentIndex length")
      }
      result
    } catch {
      case e: DataFormatException if strict =>
        throw new MalformedGeoTiffException(s"Invalid compressed TIFF segment $segmentIndex: ${e.getMessage}")
    } finally inflater.end()
  }
}
