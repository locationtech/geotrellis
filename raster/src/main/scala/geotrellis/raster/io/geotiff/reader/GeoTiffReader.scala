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

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS
import geotrellis.util.{ByteReader, Filesystem}
import monocle.syntax.apply._
import java.nio.{ByteBuffer, ByteOrder}

import geotrellis.raster.io.geotiff.tags.codes.ColorSpace
import geotrellis.raster.render.{ColorMap, IndexedColorMap, RGB}

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String)
    extends RuntimeException(msg)

object GeoTiffReader {

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String): SinglebandGeoTiff =
    readSingleband(path, true, false)

  /* Read in only the extent of a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, e: Extent): SinglebandGeoTiff =
    readSingleband(path, Some(e))

  /* Read in only the extent of a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, e: Option[Extent]): SinglebandGeoTiff =
    e match {
      case Some(x) => readSingleband(path, false, true).crop(x)
      case None => readSingleband(path)
    }

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, decompress: Boolean, streaming: Boolean): SinglebandGeoTiff =
    if (streaming)
      readSingleband(Filesystem.toMappedByteBuffer(path), decompress, streaming)
    else
      readSingleband(ByteBuffer.wrap(Filesystem.slurp(path)), decompress, streaming)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(bytes: Array[Byte]): SinglebandGeoTiff =
    readSingleband(ByteBuffer.wrap(bytes), true, false)

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(bytes: Array[Byte], decompress: Boolean,
    streaming: Boolean = false): SinglebandGeoTiff =
      readSingleband(ByteBuffer.wrap(bytes), decompress, streaming)

  def readSingleband(byteReader: ByteReader): SinglebandGeoTiff =
    readSingleband(byteReader, true, false)

  def readSingleband(byteReader: ByteReader, e: Extent): SinglebandGeoTiff =
    readSingleband(byteReader, Some(e))

  def readSingleband(byteReader: ByteReader, e: Option[Extent]): SinglebandGeoTiff =
    e match {
      case Some(x) => readSingleband(byteReader, false, true).crop(x)
      case None => readSingleband(byteReader)
    }

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(byteReader: ByteReader, decompress: Boolean, streaming: Boolean): SinglebandGeoTiff = {
    val info = readGeoTiffInfo(byteReader, decompress, streaming)

    val geoTiffTile =
      if(info.bandCount == 1) {
        GeoTiffTile(
          info.segmentBytes,
          info.decompressor,
          info.segmentLayout,
          info.compression,
          info.cellType,
          Some(info.bandType)
        )
      } else {
        GeoTiffMultibandTile(
          info.segmentBytes,
          info.decompressor,
          info.segmentLayout,
          info.compression,
          info.bandCount,
          info.hasPixelInterleave,
          info.cellType,
          Some(info.bandType)
        ).band(0)
      }

    SinglebandGeoTiff(if (decompress) geoTiffTile.toArrayTile else geoTiffTile, info.extent, info.crs, info.tags, info.options)
  }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String): MultibandGeoTiff =
    readMultiband(path, true, false)

  /* Read in only the extent for each band in a multi ban GeoTIFF file.
   */
  def readMultiband(path: String, e: Extent): MultibandGeoTiff =
    readMultiband(path, Some(e))

  /* Read in only the extent for each band in a multi ban GeoTIFF file.
   */
  def readMultiband(path: String, e: Option[Extent]): MultibandGeoTiff =
    e match {
      case Some(x) => readMultiband(path, false, true).crop(x)
      case None => readMultiband(path)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String, decompress: Boolean, streaming: Boolean): MultibandGeoTiff =
    if (streaming)
      readMultiband(Filesystem.toMappedByteBuffer(path), decompress, streaming)
    else
      readMultiband(ByteBuffer.wrap(Filesystem.slurp(path)), decompress, streaming)

  def readMultiband(byteReader: ByteReader): MultibandGeoTiff =
    readMultiband(byteReader, true, false)

  def readMultiband(byteReader: ByteReader, e: Extent): MultibandGeoTiff =
    readMultiband(byteReader, Some(e))

  def readMultiband(byteReader: ByteReader, e: Option[Extent]): MultibandGeoTiff =
    e match {
      case Some(x) => readMultiband(byteReader, false, true).crop(x)
      case None => readMultiband(byteReader)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(bytes: Array[Byte]): MultibandGeoTiff =
    readMultiband(ByteBuffer.wrap(bytes), true, false)

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(bytes: Array[Byte], decompress: Boolean,
    streaming: Boolean = false): MultibandGeoTiff =
      readMultiband(ByteBuffer.wrap(bytes), decompress, streaming)

  def readMultiband(byteReader: ByteReader, decompress: Boolean, streaming: Boolean): MultibandGeoTiff = {
    val info = readGeoTiffInfo(byteReader, decompress, streaming)

    val geoTiffTile =
      GeoTiffMultibandTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.hasPixelInterleave,
        info.cellType,
        Some(info.bandType)
      )

    new MultibandGeoTiff(if (decompress) geoTiffTile.toArrayTile else geoTiffTile, info.extent, info.crs, info.tags, info.options)
  }

  case class GeoTiffInfo(
    extent: Extent,
    crs: CRS,
    tags: Tags,
    options: GeoTiffOptions,
    bandType: BandType,
    segmentBytes: SegmentBytes,
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

  private def readGeoTiffInfo(byteReader: ByteReader, decompress: Boolean, streaming: Boolean): GeoTiffInfo = {
    // set byte ordering
    (byteReader.get.toChar, byteReader.get.toChar) match {
      case ('I', 'I') => byteReader.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteReader.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate Tiff identification number
    val tiffIdNumber = byteReader.getChar
    if (tiffIdNumber != 42 && tiffIdNumber != 43)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42 or 43, was $tiffIdNumber (${tiffIdNumber.toInt}))")

    val tiffTags =
      if (tiffIdNumber == 42) {
        val smallStart = byteReader.getInt
        TiffTagsReader.read(byteReader, smallStart)
      } else {
        byteReader.position(8)
        val bigStart = byteReader.getLong
        TiffTagsReader.read(byteReader, bigStart)
      }

    val hasPixelInterleave = tiffTags.hasPixelInterleave

    val decompressor = Decompressor(tiffTags, byteReader.order)

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

    val segmentBytes: SegmentBytes =
      if (streaming)
        LazySegmentBytes(byteReader, tiffTags)
      else
        ArraySegmentBytes(byteReader, tiffTags)

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

    val colorSpace = tiffTags.basicTags.photometricInterp

    val colorMap = if (colorSpace == ColorSpace.Palette && tiffTags.basicTags.colorMap.nonEmpty) {
      Option(IndexedColorMap.fromTiffPalette(tiffTags.basicTags.colorMap))
    } else None

    GeoTiffInfo(
      tiffTags.extent,
      tiffTags.crs,
      tiffTags.tags,
      GeoTiffOptions(storageMethod, compression, colorSpace, colorMap),
      bandType,
      segmentBytes,
      decompressor,
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      noDataValue
    )
  }
}
