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

import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.tags._

import monocle.syntax._

import scala.io._
import scala.collection.mutable
import java.nio.{ByteBuffer, ByteOrder}
import spire.syntax.cfor._

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String)
    extends RuntimeException(msg)

object GeoTiffReader {

  def readSingleBand(path: String): SingleBandGeoTiff =
    readSingleBand(path, true)

  def readSingleBand(path: String, decompress: Boolean): SingleBandGeoTiff = 
    readSingleBand(Filesystem.slurp(path), decompress)

  def readSingleBand(bytes: Array[Byte]): SingleBandGeoTiff =
    readSingleBand(bytes, true)

  def readSingleBand(bytes: Array[Byte], decompress: Boolean): SingleBandGeoTiff = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    // Set byteBuffer position
    byteBuffer.position(0)

    // set byte ordering
    (byteBuffer.get.toChar, byteBuffer.get.toChar) match {
      case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate GeoTiff identification number
    val geoTiffIdNumber = byteBuffer.getChar
    if ( geoTiffIdNumber != 42)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42, was $geoTiffIdNumber)")

    val tagsStartPosition = byteBuffer.getInt

    val tiffTags = TagsReader.read(byteBuffer, tagsStartPosition)

    val decompressor = Decompressor(tiffTags, byteBuffer.order)

    val compressedBytes: Array[Array[Byte]] = {
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

      if (tiffTags.hasStripStorage) 
        readStrips(tiffTags)
      else 
        readTiles(tiffTags)

    }
    
    val layout = GeoTiffSegmentLayout(tiffTags)
    val noDataValue = 
      (tiffTags
        &|-> Tags._geoTiffTags
        ^|-> GeoTiffTags._gdalInternalNoData get)

    // If the GeoTiff is coming is as uncompressed, leave it as uncompressed.
    // If it's any sort of compression, move forward with ZLib compression.
    val compression =
      decompressor match {
        case NoCompression => NoCompression
        case _ => ZLibCompression
      }

    val extent = tiffTags.extent
    val bandType = tiffTags.bandType
    val crs = tiffTags.crs
    val tags = tiffTags.tags
    val bandTags = tiffTags.bandTags(0)

    val geoTiffTile =
      GeoTiffTile(bandType, compressedBytes, decompressor, layout, compression, noDataValue)

    SingleBandGeoTiff(if(decompress) geoTiffTile.toArrayTile else geoTiffTile, extent, crs, tags, bandTags)
  }
}
