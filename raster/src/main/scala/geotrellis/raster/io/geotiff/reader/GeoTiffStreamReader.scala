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
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS
import geotrellis.util.Filesystem

import monocle.syntax.apply._

import scala.io._
import scala.collection.mutable
import java.nio.{ByteBuffer, ByteOrder}
import spire.syntax.cfor._


object GeoTiffStreamReader {

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String): SinglebandGeoTiff =
    readSingleband(path, true)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, decompress: Boolean): SinglebandGeoTiff =
    readSingleband(Filesystem.streamByteBuffer(path), decompress)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(bytes: ByteBuffer): SinglebandGeoTiff =
    readSingleband(bytes, true)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(bytes: ByteBuffer, decompress: Boolean): SinglebandGeoTiff = {
    val info = readGeoTiffInfo(bytes, decompress)

    val geoTiffTile =
      if(info.bandCount == 1) {
        GeoTiffTile(
          info.compressedBytes,
          info.decompressor,
          info.segmentLayout,
          info.compression,
          info.cellType,
          Some(info.bandType)
        )
      } else {
        GeoTiffMultibandTile(
          info.compressedBytes,
          info.decompressor,
          info.segmentLayout,
          info.compression,
          info.bandCount,
          info.hasPixelInterleave,
          info.cellType,
          Some(info.bandType)
        ).band(0)
      }

    SinglebandGeoTiff(if(decompress) geoTiffTile.toArrayTile else geoTiffTile, info.extent, info.crs, info.tags, info.options)
  }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String): MultibandGeoTiff =
    readMultiband(path, true)

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String, decompress: Boolean): MultibandGeoTiff =
    readMultiband(Filesystem.streamByteBuffer(path), decompress)

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(bytes: ByteBuffer): MultibandGeoTiff =
    readMultiband(bytes, true)

  def readMultiband(bytes: ByteBuffer, decompress: Boolean): MultibandGeoTiff = {
    val info = readGeoTiffInfo(bytes, decompress)
    val geoTiffTile =
      GeoTiffMultibandTile(
        info.compressedBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.hasPixelInterleave,
        info.cellType,
        Some(info.bandType)
      )

    new MultibandGeoTiff(if(decompress) geoTiffTile.toArrayTile else geoTiffTile, info.extent, info.crs, info.tags, info.options)
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
  ) {
    def cellType: CellType = (bandType, noDataValue) match {
      case (BitBandType, _) =>
        BitCellType
      // Byte
      case (ByteBandType, Some(nd)) if (nd.toInt > Byte.MinValue.toInt && nd <= Byte.MaxValue.toInt) =>
        ByteUserDefinedNoDataCellType(nd.toByte)
      case (ByteBandType, Some(nd)) if (nd.toInt == Byte.MinValue.toInt) =>
        ByteConstantNoDataCellType
      case (ByteBandType, _) =>
        ByteCellType
      // UByte
      case (UByteBandType, Some(nd)) if (nd.toInt > 0 && nd <= 255) =>
        UByteUserDefinedNoDataCellType(nd.toByte)
      case (UByteBandType, Some(nd)) if (nd.toInt == 0) =>
        UByteConstantNoDataCellType
      case (UByteBandType, _) =>
        UByteCellType
      // Int16/Short
      case (Int16BandType, Some(nd)) if (nd > Short.MinValue.toDouble && nd <= Short.MaxValue.toDouble) =>
        ShortUserDefinedNoDataCellType(nd.toShort)
      case (Int16BandType, Some(nd)) if (nd == Short.MinValue.toDouble) =>
        ShortConstantNoDataCellType
      case (Int16BandType, _) =>
        ShortCellType
      // UInt16/UShort
      case (UInt16BandType, Some(nd)) if (nd.toInt > 0 && nd <= 65535) =>
        UShortUserDefinedNoDataCellType(nd.toShort)
      case (UInt16BandType, Some(nd)) if (nd.toInt == 0) =>
        UShortConstantNoDataCellType
      case (UInt16BandType, _) =>
        UShortCellType
      // Int32
      case (Int32BandType, Some(nd)) if (nd.toInt > Int.MinValue && nd.toInt <= Int.MaxValue) =>
        IntUserDefinedNoDataCellType(nd.toInt)
      case (Int32BandType, Some(nd)) if (nd.toInt == Int.MinValue) =>
        IntConstantNoDataCellType
      case (Int32BandType, _) =>
        IntCellType
      // UInt32
      case (UInt32BandType, Some(nd)) if (nd.toLong > 0L && nd.toLong <= 4294967295L) =>
        FloatUserDefinedNoDataCellType(nd.toFloat)
      case (UInt32BandType, Some(nd)) if (nd.toLong == 0L) =>
        FloatConstantNoDataCellType
      case (UInt32BandType, _) =>
        FloatCellType
      // Float32
      case (Float32BandType, Some(nd)) if (isData(nd) & Float.MinValue.toDouble <= nd & Float.MaxValue.toDouble >= nd) =>
        FloatUserDefinedNoDataCellType(nd.toFloat)
      case (Float32BandType, Some(nd)) =>
        FloatConstantNoDataCellType
      case (Float32BandType, _) =>
        FloatCellType
      // Float64/Double
      case (Float64BandType, Some(nd)) if (isData(nd)) =>
        DoubleUserDefinedNoDataCellType(nd)
      case (Float64BandType, Some(nd)) =>
        DoubleConstantNoDataCellType
      case (Float64BandType, _) =>
        DoubleCellType
    }
  }


  private def readGeoTiffInfo(byteBuffer: ByteBuffer, decompress: Boolean): GeoTiffInfo = {

    val tiffTags = TiffTagsReader.read(byteBuffer)

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
