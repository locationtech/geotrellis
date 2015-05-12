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
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

import monocle.syntax._

import scala.io._
import scala.collection.mutable
import java.nio.{ByteBuffer, ByteOrder}
import spire.syntax.cfor._

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String)
    extends RuntimeException(msg)

// TODO: Streaming read (e.g. so that we can read GeoTiffs that cannot fit into memory. Perhaps support BigTIFF?)

object GeoTiffReader {

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleBand(path: String): SingleBandGeoTiff =
    readSingleBand(path, true)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleBand(path: String, decompress: Boolean): SingleBandGeoTiff = 
    readSingleBand(Filesystem.slurp(path), decompress)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleBand(bytes: Array[Byte]): SingleBandGeoTiff =
    readSingleBand(bytes, true)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleBand(bytes: Array[Byte], decompress: Boolean): SingleBandGeoTiff = {
    val info = readGeoTiffInfo(bytes, decompress)

    val geoTiffTile =
      if(info.bandCount == 1) {
        GeoTiffTile(
          info.bandType,
          info.compressedBytes,
          info.decompressor,
          info.segmentLayout,
          info.compression,
          info.noDataValue
        )
      } else {
        GeoTiffMultiBandTile(
          info.bandType,
          info.compressedBytes,
          info.decompressor,
          info.segmentLayout,
          info.compression,
          info.bandCount,
          info.hasPixelInterleave,
          info.noDataValue
        ).band(0)
      }

    SingleBandGeoTiff(if(decompress) geoTiffTile.toArrayTile else geoTiffTile, info.extent, info.crs, info.tags, info.options)
  }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiBand(path: String): MultiBandGeoTiff =
    readMultiBand(path, true)

  /* Read a multi band GeoTIFF file.
   */
  def readMultiBand(path: String, decompress: Boolean): MultiBandGeoTiff = 
    readMultiBand(Filesystem.slurp(path), decompress)

  /* Read a multi band GeoTIFF file.
   */
  def readMultiBand(bytes: Array[Byte]): MultiBandGeoTiff =
    readMultiBand(bytes, true)

  def readMultiBand(bytes: Array[Byte], decompress: Boolean): MultiBandGeoTiff = {
    val info = readGeoTiffInfo(bytes, decompress)
    val tile =
      GeoTiffMultiBandTile(
        info.bandType,
        info.compressedBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.hasPixelInterleave,
        info.noDataValue
      )

    new MultiBandGeoTiff(tile, info.extent, info.crs, info.tags, info.options)
  }

  case class GeoTiffInfo(
    extent: Extent,
    crs: CRS,
    tags: Tags,
    options: GeoTiffOptions,
    bandType: BandType,
    compressedBytes: Array[Array[Byte]],
    decompressor: Decompressor,
    segmentLayout: GeoTiffSegmentLayout,
    compression: Compression,
    bandCount: Int,
    hasPixelInterleave: Boolean,
    noDataValue: Option[Double]
  )

  private def readGeoTiffInfo(bytes: Array[Byte], decompress: Boolean): GeoTiffInfo = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    // Set byteBuffer position
    byteBuffer.position(0)

    // set byte ordering
    (byteBuffer.get.toChar, byteBuffer.get.toChar) match {
      case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate Tiff identification number
    val tiffIdNumber = byteBuffer.getChar
    if (tiffIdNumber != 42)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42, was $tiffIdNumber)")

    val tagsStartPosition = byteBuffer.getInt

    val tiffTags = TiffTagsReader.read(byteBuffer, tagsStartPosition)

    val hasPixelInterleave = tiffTags.hasPixelInterleave

    val decompressor = Decompressor(tiffTags, byteBuffer.order)

    val storageMethod: StorageMethod =
      if(tiffTags.hasStripStorage) {
        val rowsPerStrip: Int =
          (tiffTags
            &|-> TiffTags._basicTags
            ^|-> BasicTags._rowsPerStrip get).toInt

        Striped(rowsPerStrip)
      } else {
        val blockCols =
          (tiffTags
            &|-> TiffTags._tileTags
            ^|-> TileTags._tileWidth get).get.toInt

        val blockRows =
          (tiffTags
            &|-> TiffTags._tileTags
            ^|-> TileTags._tileLength get).get.toInt

        Tiled(blockCols, blockRows)
      }

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

      storageMethod match {
        case _: Striped =>

          val stripOffsets = (tiffTags &|->
            TiffTags._basicTags ^|->
            BasicTags._stripOffsets get)

          val stripByteCounts = (tiffTags &|->
            TiffTags._basicTags ^|->
            BasicTags._stripByteCounts get)

          readSections(stripOffsets.get, stripByteCounts.get)

        case _: Tiled =>
          val tileOffsets = (tiffTags &|->
            TiffTags._tileTags ^|->
            TileTags._tileOffsets get)

          val tileByteCounts = (tiffTags &|->
            TiffTags._tileTags ^|->
            TileTags._tileByteCounts get)

          readSections(tileOffsets.get, tileByteCounts.get)
      }
    }

    val cols = tiffTags.cols
    val rows = tiffTags.rows
    val bandType = tiffTags.bandType
    val bandCount = tiffTags.bandCount
    
    val segmentLayout = GeoTiffSegmentLayout(cols, rows, storageMethod, bandType)
    val noDataValue = 
      (tiffTags
        &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._gdalInternalNoData get)

    // If the GeoTiff is coming is as uncompressed, leave it as uncompressed.
    // If it's any sort of compression, move forward with ZLib compression.
    val compression =
      decompressor match {
        case NoCompression => NoCompression
        case _ => DeflateCompression
      }

    GeoTiffInfo(
      tiffTags.extent,
      tiffTags.crs,
      tiffTags.tags,
      GeoTiffOptions(storageMethod, compression),
      bandType,
      compressedBytes,
      decompressor,
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      noDataValue
    )
  }
}
