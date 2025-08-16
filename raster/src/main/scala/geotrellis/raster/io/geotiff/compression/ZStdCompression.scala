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

import com.github.luben.zstd.{ZstdInputStream, ZstdOutputStream}
import org.apache.commons.io.IOUtils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

case class ZStdCompression(level: Int = 3) extends Compression {
  def createCompressor(segmentCount: Int): Compressor =
    new ZStdCompressor(segmentCount, level)

  def createDecompressor(segmentSizes: Array[Int]): Decompressor =
    new ZStdDecompressor(segmentSizes)

}

object ZStdCompression extends ZStdCompression(3)

class ZStdCompressor(segmentCount: Int, level: Int) extends Compressor {
  private val segmentSizes = Array.ofDim[Int](segmentCount)

  def compress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    segmentSizes(segmentIndex) = segment.size

    val outputStream = new ByteArrayOutputStream()
    val compressorOutputStream = new ZstdOutputStream(outputStream, level)
    IOUtils.copyLarge(new ByteArrayInputStream(segment), compressorOutputStream)
    compressorOutputStream.close()
    outputStream.toByteArray
  }

  def createDecompressor(): Decompressor =
    new ZStdDecompressor(segmentSizes)
}

class ZStdDecompressor(segmentSizes: Array[Int]) extends Decompressor {
  def code = ZstdCoded

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    val stream = new ByteArrayInputStream(segment)
    val compressorInputStream = new ZstdInputStream(stream)
    IOUtils.copyLarge(compressorInputStream, outputStream)
    compressorInputStream.close()
    outputStream.toByteArray
  }
}

