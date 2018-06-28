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

import java.io.File

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS
import geotrellis.util.{ByteReader, Filesystem, FileRangeReader, StreamingByteReader}
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace
import geotrellis.raster.render.IndexedColorMap
import monocle.syntax.apply._
import java.nio.{ByteBuffer, ByteOrder}

import scala.collection.mutable.ListBuffer

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String) extends RuntimeException(msg)

object GeoTiffReader {

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String): SinglebandGeoTiff =
    readSingleband(path, false)

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
      case Some(x) => readSingleband(path, true).crop(x)
      case None => readSingleband(path)
    }

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, streaming: Boolean): SinglebandGeoTiff = {
    val ovrPath = s"${path}.ovr"
    val ovrPathExists = new File(ovrPath).isFile
    if (streaming)
      readSingleband(
        StreamingByteReader(FileRangeReader(path)),
        streaming, true,
        if(ovrPathExists) Some(StreamingByteReader(FileRangeReader(ovrPath))) else None
      )
    else
      readSingleband(
        ByteBuffer.wrap(Filesystem.slurp(path)),
        streaming, true,
        if(ovrPathExists) Some(ByteBuffer.wrap(Filesystem.slurp(ovrPath))) else None
      )
  }

  def readSingleband(bytes: Array[Byte]): SinglebandGeoTiff =
    readSingleband(ByteBuffer.wrap(bytes), false)

  def readSingleband(bytes: Array[Byte], streaming: Boolean): SinglebandGeoTiff =
    readSingleband(ByteBuffer.wrap(bytes), streaming)

  def readSingleband(byteReader: ByteReader): SinglebandGeoTiff =
    readSingleband(byteReader, false)

  def readSingleband(byteReader: ByteReader, e: Extent): SinglebandGeoTiff =
    readSingleband(byteReader, Some(e))

  def readSingleband(byteReader: ByteReader, e: Option[Extent]): SinglebandGeoTiff =
    e match {
      case Some(x) => readSingleband(byteReader, true).crop(x)
      case None => readSingleband(byteReader)
    }

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(byteReader: ByteReader, streaming: Boolean): SinglebandGeoTiff =
    readSingleband(byteReader, streaming, true, None)

  def readSingleband(byteReader: ByteReader, streaming: Boolean, withOverviews: Boolean, byteReaderExternal: Option[ByteReader]): SinglebandGeoTiff = {
    def getSingleband(geoTiffTile: GeoTiffTile, info: GeoTiffInfo): SinglebandGeoTiff =
      SinglebandGeoTiff(
        geoTiffTile,
        info.extent,
        info.crs,
        info.tags,
        info.options,
        info.overviews.map { i => getSingleband(geoTiffSinglebandTile(i), i) }
      )

    val info = readGeoTiffInfo(byteReader, streaming, withOverviews, byteReaderExternal)
    val geoTiffTile = geoTiffSinglebandTile(info)

    getSingleband(geoTiffTile, info)
  }

  def geoTiffSinglebandTile(info: GeoTiffInfo): GeoTiffTile =
    if(info.bandCount == 1) {
      GeoTiffTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.cellType,
        Some(info.bandType),
        info.overviews.map(geoTiffSinglebandTile)
      )
    } else {
      GeoTiffMultibandTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.cellType,
        Some(info.bandType),
        info.overviews.map(geoTiffMultibandTile)
      ).band(0)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String): MultibandGeoTiff =
    readMultiband(path, false)

  /* Read in only the extent for each band in a multi ban GeoTIFF file.
   */
  def readMultiband(path: String, e: Extent): MultibandGeoTiff =
    readMultiband(path, Some(e))

  /* Read in only the extent for each band in a multi ban GeoTIFF file.
   */
  def readMultiband(path: String, e: Option[Extent]): MultibandGeoTiff =
    e match {
      case Some(x) => readMultiband(path, true).crop(x)
      case None => readMultiband(path)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String, streaming: Boolean): MultibandGeoTiff = {
    val ovrPath = s"${path}.ovr"
    val ovrPathExists = new File(ovrPath).isFile
    if (streaming)
      readMultiband(
        StreamingByteReader(FileRangeReader(path)),
        streaming, true,
        if(ovrPathExists) Some(StreamingByteReader(FileRangeReader(ovrPath))) else None
      )
    else
      readMultiband(
        ByteBuffer.wrap(Filesystem.slurp(path)),
        streaming, true,
        if(ovrPathExists) Some(ByteBuffer.wrap(Filesystem.slurp(ovrPath))) else None
      )
  }

  def readMultiband(byteReader: ByteReader, e: Extent): MultibandGeoTiff =
    readMultiband(byteReader, Some(e))

  def readMultiband(byteReader: ByteReader, e: Option[Extent]): MultibandGeoTiff =
    e match {
      case Some(x) => readMultiband(byteReader, true).crop(x)
      case None => readMultiband(byteReader)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(bytes: Array[Byte]): MultibandGeoTiff =
    readMultiband(bytes, false)

  def readMultiband(bytes: Array[Byte], streaming: Boolean): MultibandGeoTiff =
    readMultiband(ByteBuffer.wrap(bytes), streaming)

  def readMultiband(byteReader: ByteReader): MultibandGeoTiff =
    readMultiband(byteReader, false)

  def readMultiband(byteReader: ByteReader, streaming: Boolean): MultibandGeoTiff =
    readMultiband(byteReader, streaming, true, None)

  def readMultiband(byteReader: ByteReader, streaming: Boolean, withOverviews: Boolean, byteReaderExternal: Option[ByteReader]): MultibandGeoTiff = {
    def getMultiband(geoTiffTile: GeoTiffMultibandTile, info: GeoTiffInfo): MultibandGeoTiff =
      new MultibandGeoTiff(
        geoTiffTile,
        info.extent,
        info.crs,
        info.tags,
        info.options,
        info.overviews.map { i => getMultiband(geoTiffMultibandTile(i), i) }
      )

    val info = readGeoTiffInfo(byteReader, streaming, withOverviews, byteReaderExternal)
    val geoTiffTile = geoTiffMultibandTile(info)

    getMultiband(geoTiffTile, info)
  }

    def geoTiffMultibandTile(info: GeoTiffInfo): GeoTiffMultibandTile = {
      GeoTiffMultibandTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.cellType,
        Some(info.bandType),
        info.overviews.map(geoTiffMultibandTile)
      )
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
    noDataValue: Option[Double],
    overviews: List[GeoTiffInfo] = Nil
  ) {
    def rasterExtent: RasterExtent =
      RasterExtent(extent = extent, cols = segmentLayout.totalCols, rows = segmentLayout.totalRows)

    // converts GeoTiffInfo into list with Nil overviews
    def toList: List[GeoTiffInfo] = this.copy(overviews = Nil) :: overviews

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

    def getOverviewsCount: Int = overviews.length
    def getOverview(idx: Int): GeoTiffInfo = overviews(idx)
  }

  def readGeoTiffInfo(
    byteReader: ByteReader,
    streaming: Boolean,
    withOverviews: Boolean
  ): GeoTiffInfo = readGeoTiffInfo(byteReader, streaming, withOverviews, None)

  def readGeoTiffInfo(
    byteReader: ByteReader,
    streaming: Boolean,
    withOverviews: Boolean,
    byteReaderExternal: Option[ByteReader]
  ): GeoTiffInfo = {
    val oldPos = byteReader.position
    try {
      byteReader.position(0)
      // set byte ordering
      (byteReader.get.toChar, byteReader.get.toChar) match {
        case ('I', 'I') =>
          byteReader.order(ByteOrder.LITTLE_ENDIAN)
        case ('M', 'M') =>
          byteReader.order(ByteOrder.BIG_ENDIAN)
        case _ => throw new MalformedGeoTiffException("incorrect byte order")
      }

      byteReader.position(oldPos + 2)
      // Validate Tiff identification number
      val tiffIdNumber = byteReader.getChar
      if (tiffIdNumber != 42 && tiffIdNumber != 43)
        throw new MalformedGeoTiffException(s"bad identification number (must be 42 or 43, was $tiffIdNumber (${tiffIdNumber.toInt}))")

      val tiffType = TiffType.fromCode(tiffIdNumber)

      val baseTiffTags: TiffTags =
        tiffType match {
          case Tiff =>
            val smallStart = byteReader.getInt
            TiffTagsReader.read(byteReader, smallStart.toLong)(IntTiffTagOffsetSize)
          case _ =>
            byteReader.position(8)
            val bigStart = byteReader.getLong
            TiffTagsReader.read(byteReader, bigStart)(LongTiffTagOffsetSize)
        }

      // IFD overviews may contain not all tags required for a proper work with it
      // for instance it may not contain CRS metadata
      val tiffTagsList: List[TiffTags] = {
        val tiffTagsBuffer: ListBuffer[TiffTags] = ListBuffer()
        if(withOverviews) {
          tiffType match {
            case Tiff =>
              var ifdOffset = byteReader.getInt
              while (ifdOffset > 0) {
                tiffTagsBuffer += TiffTagsReader.read(byteReader, ifdOffset)(IntTiffTagOffsetSize)
                ifdOffset = byteReader.getInt
              }
            case _ =>
              var ifdOffset = byteReader.getLong
              while (ifdOffset > 0) {
                tiffTagsBuffer += TiffTagsReader.read(byteReader, ifdOffset)(LongTiffTagOffsetSize)
                ifdOffset = byteReader.getLong
              }
          }
        }
        tiffTagsBuffer.toList
      }

      def getGeoTiffInfo(tiffTags: TiffTags, overviews: List[GeoTiffInfo] = Nil): GeoTiffInfo = {
        val interleaveMethod = tiffTags.interleaveMethod

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

        val cols = tiffTags.cols
        val rows = tiffTags.rows
        val bandType = tiffTags.bandType
        val bandCount = tiffTags.bandCount

        val segmentLayout = GeoTiffSegmentLayout(cols, rows, storageMethod, interleaveMethod, bandType)

        val segmentBytes: SegmentBytes =
          if (streaming)
            LazySegmentBytes(byteReader, tiffTags)
          else
            ArraySegmentBytes(byteReader, tiffTags)

        val noDataValue =
          (tiffTags
            &|-> TiffTags._geoTiffTags
            ^|-> GeoTiffTags._gdalInternalNoData get)

        val subfileType =
          (tiffTags
            &|-> TiffTags._nonBasicTags ^|->
            NonBasicTags._newSubfileType get).flatMap(code => NewSubfileType.fromCode(code))

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

        // overviews can have no extent and crs, but it's required for most of our operations
        GeoTiffInfo(
          baseTiffTags.extent,
          baseTiffTags.crs,
          tiffTags.tags,
          GeoTiffOptions(storageMethod, compression, colorSpace, colorMap, interleaveMethod, subfileType, tiffType),
          bandType,
          segmentBytes,
          decompressor,
          segmentLayout,
          compression,
          bandCount,
          noDataValue,
          overviews
        )
      }

      val overviews: List[GeoTiffInfo] = {
        val list = tiffTagsList.map(getGeoTiffInfo(_))
        /** if there are no internal overviews, try to find external */
        if(tiffTagsList.isEmpty && withOverviews)
          byteReaderExternal
            .map { reader =>
              readGeoTiffInfo(reader, streaming, withOverviews, None)
                .toList // overviews can have no extent and crs, but it's required for most of our operations
                .map { _.copy(extent = baseTiffTags.extent, crs = baseTiffTags.crs) }
            }
            .getOrElse(list)
        else list
      }

      getGeoTiffInfo(baseTiffTags, overviews)
    } finally {
      byteReader.position(oldPos)
    }
  }

  implicit val singlebandGeoTiffReader: GeoTiffReader[Tile] = new GeoTiffReader[Tile]{
    def read(byteReader: ByteReader, streaming: Boolean): GeoTiff[Tile] =
      GeoTiffReader.readSingleband(byteReader, streaming)
  }

  implicit val multibandGeoTiffReader: GeoTiffReader[MultibandTile] = new GeoTiffReader[MultibandTile]{
    def read(byteReader: ByteReader, streaming: Boolean): GeoTiff[MultibandTile] =
      GeoTiffReader.readMultiband(byteReader, streaming)
  }

  def apply[V <: CellGrid](implicit ev: GeoTiffReader[V]): GeoTiffReader[V] = ev
}

trait GeoTiffReader[V <: CellGrid] extends Serializable {
  def read(byteReader: ByteReader, streaming: Boolean): GeoTiff[V]

  def read(bytes: Array[Byte]): GeoTiff[V] =
    read(ByteBuffer.wrap(bytes), streaming = false)
}
