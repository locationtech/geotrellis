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

package geotrellis.raster.io.geotiff.reader

import monocle.syntax._
import monocle.Macro._

import java.nio.ByteBuffer

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.reader.CompressionType._
import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._

import geotrellis.raster.io.geotiff.reader.utils.ByteBufferUtils._

import geotrellis.raster.io.geotiff.reader.decompression.HuffmanDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.GroupThreeDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.GroupFourDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.GroupFourDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.LZWDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.JpegDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.ZLibDecompression._
import geotrellis.raster.io.geotiff.reader.decompression.PackBitsDecompression._

case class ImageReader(byteBuffer: ByteBuffer) {

  def read(directory: ImageDirectory): ImageDirectory = {
    val matrix = readMatrix(directory)

    val uncompressedImage = directory |-> compressionLens get match {
      case Uncompressed => matrix
      case HuffmanCoded => matrix.uncompressHuffman(directory)
      case GroupThreeCoded => matrix.uncompressGroupThree(directory)
      case GroupFourCoded => matrix.uncompressGroupFour(directory)
      case LZWCoded => matrix.uncompressLZW(directory)
      case JpegOldCoded => throw new MalformedGeoTiffException(
        "old jpeg (compression = 6) is deprecated."
      )
      case JpegCoded => matrix.uncompressJpeg(directory)
      case ZLibCoded => matrix.uncompressZLib(directory)
      case PackBitsCoded => matrix.uncompressPackBits(directory)
      case compression => throw new GeoTiffReaderLimitationException(
        s"compression type $compression is not supported by this reader."
      )
    }

    val imageBytes = ImageConverter(directory).convert(uncompressedImage)

    directory |-> imageBytesLens set(imageBytes)
  }

  def readMatrix(directory: ImageDirectory): Vector[Vector[Byte]] =
    if (directory.hasStripStorage) readStrips(directory)
    else readTiles(directory)

  def readStrips(directory: ImageDirectory): Vector[Vector[Byte]] = {
    val stripOffsets = directory |-> stripOffsetsLens get
    val stripByteCounts = directory |-> stripByteCountsLens get

    readSections(stripOffsets.get, stripByteCounts.get)
  }

  private def readTiles(directory: ImageDirectory) = {
    val tileOffsets = directory |-> tileOffsetsLens get
    val tileByteCounts = directory |-> tileByteCountsLens get

    readSections(tileOffsets.get, tileByteCounts.get)
  }

  private def readSections(offsets: Vector[Int], byteCounts: Vector[Int]) = {
    val oldOffset = byteBuffer.position

    val matrix = (for (i <- 0 until offsets.size) yield {
      byteBuffer.position(offsets(i))
      byteBuffer.getSignedByteVector(byteCounts(i))
    }).toVector

    byteBuffer.position(oldOffset)

    matrix
  }
}
