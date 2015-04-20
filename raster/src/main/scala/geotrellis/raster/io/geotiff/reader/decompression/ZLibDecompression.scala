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

package geotrellis.raster.io.geotiff.reader.decompression

import java.util.zip.{ Inflater, Deflater }

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags.Tags

import scala.collection.mutable

import spire.syntax.cfor._

object ZLibCompression extends Compression {
  def createCompressor(segmentCount: Int): Compressor = 
    new Compressor {
      private val segmentSizes = Array.ofDim[Int](segmentCount)
      def compress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
        segmentSizes(segmentIndex) = segment.size

        val deflater = new Deflater()
        val tmp = segment.clone
        deflater.setInput(segment, 0, segment.length)
        deflater.finish()
        val compressedSize = deflater.deflate(tmp)
        val result = Array.ofDim[Byte](compressedSize)
        System.arraycopy(tmp, 0, result, 0, compressedSize)
        result
      }

      def createDecompressor(): Decompressor =
        new ZLibDecompressor(segmentSizes)
    }

  def createDecompressor(tags: Tags): ZLibDecompressor = {
    val segmentCount = tags.segmentCount
    val segmentSizes = Array.ofDim[Int](segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { i =>
      segmentSizes(i) = tags.imageSegmentByteSize(i).toInt
    }

    new ZLibDecompressor(segmentSizes)
  }
}

class ZLibDecompressor(segmentSizes: Array[Int]) extends Decompressor {
  def compress(segment: Array[Byte]): Array[Byte] = {
    val deflater = new Deflater()
    val tmp = segment.clone
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

trait ZLibDecompression {

  implicit class ZLib(matrix: Array[Array[Byte]]) {
    def uncompressZLib(tags: Tags): Array[Array[Byte]] = {
      val decompressor = ZLibCompression.createDecompressor(tags)
      val len = matrix.length
      val arr = Array.ofDim[Array[Byte]](len)

      cfor(0)(_ < len, _ + 1) { i =>
        val segment = matrix(i)

        arr(i) = decompressor.decompress(segment, i)
      }

      arr
    }
  }

}
