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

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.tags.codes.CompressionType._
import geotrellis.raster.io.geotiff.reader.decompression._

import monocle.syntax._

import java.nio.{ByteBuffer, ByteOrder}

import spire.syntax.cfor._

object ImageReader {

  def read(byteBuffer: ByteBuffer, tags: Tags): Array[Byte] = {

    def readSections(offsets: Array[Int], byteCounts: Array[Int]): Array[Array[Byte]] = {
      val oldOffset = byteBuffer.position

      val result = Array.ofDim[Array[Byte]](offsets.size)

      cfor(0)(_ < offsets.size, _ + 1) { i =>
        byteBuffer.position(offsets(i))
        result(i) = byteBuffer.getSignedByteArray(byteCounts(i))
      }

      byteBuffer.position(oldOffset)

      result
    }

    def readMatrix(tags: Tags): Array[Array[Byte]] =
      if (tags.hasStripStorage) readStrips(tags)
      else readTiles(tags)

    def readStrips(tags: Tags): Array[Array[Byte]] = {
      val stripOffsets = (tags &|->
        Tags._basicTags ^|->
        BasicTags._stripOffsets get)

      val stripByteCounts = (tags &|->
        Tags._basicTags ^|->
        BasicTags._stripByteCounts get)

      readSections(stripOffsets.get, stripByteCounts.get)
    }

    def readTiles(tags: Tags) = {
      val tileOffsets = (tags &|->
        Tags._tileTags ^|->
        TileTags._tileOffsets get)

      val tileByteCounts = (tags &|->
        Tags._tileTags ^|->
        TileTags._tileByteCounts get)

      readSections(tileOffsets.get, tileByteCounts.get)
    }

    val matrix = readMatrix(tags)

    val uncompressedImage: Array[Array[Byte]] =
      tags.compression match {
        case Uncompressed => matrix
        case LZWCoded => matrix.uncompressLZW(tags)
        case ZLibCoded | PkZipCoded => matrix.uncompressZLib(tags)
        case PackBitsCoded => matrix.uncompressPackBits(tags)

        // Unsupported compression types
        case JpegCoded => 
          val msg = "compression type JPEG is not supported by this reader."
          throw new GeoTiffReaderLimitationException(msg)
        case HuffmanCoded => 
          val msg = "compression type CCITTRLE is not supported by this reader."
          throw new GeoTiffReaderLimitationException(msg)
        case GroupThreeCoded => 
          val msg = s"compression type CCITTFAX3 is not supported by this reader."
          throw new GeoTiffReaderLimitationException(msg)
        case GroupFourCoded => 
          val msg = s"compression type CCITTFAX4 is not supported by this reader."
          throw new GeoTiffReaderLimitationException(msg)
        case JpegOldCoded =>
          val msg = "old jpeg (compression = 6) is deprecated."
          throw new MalformedGeoTiffException(msg)
        case compression =>
          val msg = s"compression type $compression is not supported by this reader."
          throw new GeoTiffReaderLimitationException(msg)
      }


    val imageBytes = ImageConverter(tags,
      byteBuffer.order == ByteOrder.BIG_ENDIAN).convert(uncompressedImage)

    imageBytes
  }

}
