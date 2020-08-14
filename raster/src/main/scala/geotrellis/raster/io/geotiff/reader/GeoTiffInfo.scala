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

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.proj4.CRS
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace
import geotrellis.raster.render.IndexedColorMap
import geotrellis.util.ByteReader
import monocle.syntax.apply._
import java.nio.ByteOrder
import scala.collection.mutable.ListBuffer

/** Container for GeoTiff metadata read by [[GeoTiffReader]] */
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

  /** Build list with head as this object and tail as overviews field */
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
}

object GeoTiffInfo {
  def read(
    byteReader: ByteReader,
    streaming: Boolean,
    withOverviews: Boolean,
    byteReaderExternal: Option[ByteReader] = None
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
            TiffTags.read(byteReader, smallStart.toLong)(IntTiffTagOffsetSize)
          case _ =>
            byteReader.position(8)
            val bigStart = byteReader.getLong
            TiffTags.read(byteReader, bigStart)(LongTiffTagOffsetSize)
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
                val ifdTiffTags = TiffTags.read(byteReader, ifdOffset)(IntTiffTagOffsetSize)
                // TIFF Reader supports only overviews at this point
                // Overview is a reduced-resolution IFD
                val subfileType = ifdTiffTags.nonBasicTags.newSubfileType.flatMap(NewSubfileType.fromCode)
                if(subfileType.contains(ReducedImage)) tiffTagsBuffer += ifdTiffTags
                ifdOffset = byteReader.getInt
              }
            case _ =>
              var ifdOffset = byteReader.getLong
              while (ifdOffset > 0) {
                val ifdTiffTags = TiffTags.read(byteReader, ifdOffset)(LongTiffTagOffsetSize)
                // TIFF Reader supports only overviews at this point
                // Overview is a reduced-resolution IFD
                val subfileType = ifdTiffTags.nonBasicTags.newSubfileType.flatMap(NewSubfileType.fromCode)
                if(subfileType.contains(ReducedImage)) tiffTagsBuffer += ifdTiffTags
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
              GeoTiffInfo.read(reader, streaming, withOverviews, None)
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
}