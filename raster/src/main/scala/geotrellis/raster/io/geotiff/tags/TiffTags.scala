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

package geotrellis.raster.io.geotiff.tags

import io.circe._
import io.circe.generic.semiauto._
import geotrellis.proj4.{CRS, LatLng}
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.{MalformedGeoTiffException, GeoTiffCSParser}
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.tags.codes._
import geotrellis.raster.io.geotiff.util._
import geotrellis.util.{ByteReader, Filesystem}
import java.nio.{ByteBuffer, ByteOrder}
import ModelTypes._
import monocle.macros.Lenses
import monocle.syntax.apply._
import ProjectionTypesMap.UserDefinedProjectionType
import spire.syntax.cfor._
import TagCodes._
import TiffFieldType._
import xml._

@Lenses("_")
case class TiffTags(
  metadataTags: MetadataTags = MetadataTags(),
  basicTags: BasicTags = BasicTags(),
  nonBasicTags: NonBasicTags = NonBasicTags(),
  geoTiffTags: GeoTiffTags = GeoTiffTags(),
  documentationTags: DocumentationTags = DocumentationTags(),
  tileTags: TileTags = TileTags(),
  cmykTags: CmykTags = CmykTags(),
  dataSampleFormatTags: DataSampleFormatTags = DataSampleFormatTags(),
  colimetryTags: ColimetryTags = ColimetryTags(),
  jpegTags: JpegTags = JpegTags(),
  yCbCrTags: YCbCrTags = YCbCrTags(),
  nonStandardizedTags: NonStandardizedTags = NonStandardizedTags(),
  tiffType: TiffType = Tiff,
  overviews: List[TiffTags] = Nil
) {
  def rasterExtent: RasterExtent = RasterExtent(extent, cols, rows)

  def segmentOffsets: Array[Long] =
    if (this.hasStripStorage)
      (this &|->
        TiffTags._basicTags ^|->
        BasicTags._stripOffsets get).get
    else
      (this &|->
        TiffTags._tileTags ^|->
        TileTags._tileOffsets get).get

  def segmentByteCounts: Array[Long] =
    if (this.hasStripStorage)
      (this &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts get).get
    else
      (this &|->
        TiffTags._tileTags ^|->
        TileTags._tileByteCounts get).get


  def storageMethod: StorageMethod =
    if(hasStripStorage) {
      val rowsPerStrip: Int =
        (this
          &|-> TiffTags._basicTags
          ^|-> BasicTags._rowsPerStrip get).toInt

      Striped(rowsPerStrip)
    } else {
      val blockCols =
        (this
          &|-> TiffTags._tileTags
          ^|-> TileTags._tileWidth get).get.toInt

      val blockRows =
        (this
          &|-> TiffTags._tileTags
          ^|-> TileTags._tileLength get).get.toInt

      Tiled(blockCols, blockRows)
    }

  def geoTiffSegmentLayout: GeoTiffSegmentLayout =
    GeoTiffSegmentLayout(this.cols, this.rows, this.storageMethod, this.interleaveMethod, this.bandType)

  def cellSize =
    CellSize(this.extent.width / this.cols, this.extent.height / this.rows)

  def compression =
    (this
      &|-> TiffTags._basicTags
      ^|-> BasicTags._compression get)

  def hasStripStorage(): Boolean =
    (this
      &|-> TiffTags._tileTags
      ^|-> TileTags._tileWidth get).isEmpty

  def interleaveMethod(): InterleaveMethod =
    (this
      &|-> TiffTags._nonBasicTags
      ^|-> NonBasicTags._planarConfiguration get) match {
      case Some(PlanarConfigurations.PixelInterleave) =>
        PixelInterleave
      case Some(PlanarConfigurations.BandInterleave) =>
        BandInterleave
      case None =>
        PixelInterleave
      case Some(i) =>
          throw new MalformedGeoTiffException(s"Bad PlanarConfiguration tag: $i")
    }

  def hasPixelInterleave: Boolean =
    interleaveMethod == PixelInterleave

  def rowsInStrip(index: Int): Option[Long] =
    if (hasStripStorage) {
      (this &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) => {
          val rowsPerStrip = (this &|->
            TiffTags._basicTags ^|->
            BasicTags._rowsPerStrip get)
          val imageLength = rows
          val numberOfStrips = stripByteCounts.size
          val rest = imageLength % rowsPerStrip

          if (index == numberOfStrips - 1) {
            Some(if (rest == 0) rowsPerStrip else rest)
          } else if (index >= 0 && index < numberOfStrips - 1) {
            Some(rowsPerStrip)
          } else {
            throw new IllegalArgumentException("index is bad.")
          }
        }
        case None =>
          throw new MalformedGeoTiffException("bad rows/tile structure")
      }
    } else {
      None
    }

  def rowsInSegment(index: Int): Int =
    if (hasStripStorage)
      rowsInStrip(index).get.toInt
    else
      (this &|->
        TiffTags._tileTags ^|->
        TileTags._tileLength get).get.toInt

  def bitsPerPixel(): Int =
    bitsPerSample * bandCount

  def bytesPerPixel: Int =
    (this.bitsPerPixel + 7) / 8

  def bitsPerSample: Int =
    (this
      &|-> TiffTags._basicTags
      ^|-> BasicTags._bitsPerSample get)

  def imageSegmentByteSize(index: Int): Long =
    {(imageSegmentBitsSize(index) + 7) / 8 }

  def imageSegmentBitsSize(index: Int): Long =
    if (hasStripStorage) {
      val c = {
        // For 1 bit rasters, take into account
        // that the rows are padded with extra bits to make
        // up the last byte.
        if(bitsPerPixel == 1) {
          val m = (cols + 7) / 8
          8 * m
        } else {
          cols
        }
      }

      (rowsInStrip(index).get * c * bitsPerPixel) / bandCount
    }
    else {
      // We don't need the same check for 1 bit rasters as above,
      // because according the the TIFF 6.0 Spec, "TileWidth must be a multiple of 16".
      (
        (this &|->
          TiffTags._tileTags ^|->
          TileTags._tileWidth get),
        (this &|->
          TiffTags._tileTags ^|->
          TileTags._tileLength get)
      ) match {
        case (Some(tileWidth), Some(tileHeight)) =>
          (bitsPerPixel * tileWidth * tileHeight) / bandCount
        case _ =>
          throw new MalformedGeoTiffException("Cannot find TileWidth and TileLength tags for tiled GeoTiff.")
      }
    }

  def rowSize: Int =
    if (hasStripStorage) cols
    else (this &|-> TiffTags._tileTags ^|-> TileTags._tileWidth get).get.toInt

  def cols = (this &|-> TiffTags._basicTags ^|-> BasicTags._imageWidth get)
  def rows = (this &|-> TiffTags._basicTags ^|-> BasicTags._imageLength get)

  def extent: Extent =
    (this
      &|-> TiffTags._geoTiffTags
      ^|-> GeoTiffTags._modelTransformation get
    ) match {
      case Some(trans) =>
        assert(trans.size == 4 && trans(0).size == 4, "Malformed model transformation matrix (must be a 4 x 4 matrix)")

        getExtentFromModelFunction { pixel =>
          val transformed = Array.ofDim[Double](3)
          cfor(0)(_ < 3, _ + 1) { row =>
            transformed(row) =
              trans(row)(0) * pixel.x + trans(row)(1) * pixel.y + trans(row)(2) * pixel.z + trans(row)(3)
          }

          Pixel3D.fromArray(transformed)
        }
      case _ =>
        (this
          &|-> TiffTags._geoTiffTags
          ^|-> GeoTiffTags._modelTiePoints get
        ) match {
          case Some(tiePoints) if (!tiePoints.isEmpty) =>
            tiePointsModelSpace(
              tiePoints,
              (this
                &|-> TiffTags._geoTiffTags
                ^|-> GeoTiffTags._modelPixelScale get
              )
            )
          case _ =>
            Extent(0, 0, cols, rows)
        }
    }

  def bandType: BandType = {
    val sampleFormat =
      (this
        &|-> TiffTags._dataSampleFormatTags
        ^|-> DataSampleFormatTags._sampleFormat get)

    BandType(bitsPerSample, sampleFormat)
  }

  def noDataValue =
    (this
      &|-> TiffTags._geoTiffTags
      ^|-> GeoTiffTags._gdalInternalNoData get)

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

  def proj4String: Option[String] =
    geoTiffCSTags.flatMap(_.getProj4String)

  lazy val crs: CRS = {
    val fromCode: Option[CRS] =
      geoTiffCSTags.flatMap { csTags =>
        csTags.model match {
          case ModelTypeProjected =>
            val pcs = csTags.pcs
            if (pcs != UserDefinedProjectionType)
              Some(CRS.fromName(s"EPSG:${pcs}"))
            else
              None
          case ModelTypeGeographic =>
            val gcs = csTags.gcs
            if (gcs != UserDefinedProjectionType)
              Some(CRS.fromName(s"EPSG:${gcs}"))
            else
              None
          case _ => None
        }
      }
    fromCode.getOrElse({
      proj4String match {
        case Some(s) => CRS.fromString(s)
        case None => LatLng
      }
    })
  }

  private def getRasterBoundaries: Array[Pixel3D] = {
    val imageWidth = cols
    val imageLength = rows

    Array(
      Pixel3D(0, imageLength, 0),
      Pixel3D(imageWidth, 0, 0)
    )
  }

  private def tiePointsModelSpace(
    tiePoints: Array[(Pixel3D, Pixel3D)],
    pixelScaleOption: Option[(Double, Double, Double)]
  ) =
    pixelScaleOption match {
      case Some(pixelScales) =>
        def modelFunc(pixel: Pixel3D) = {
          val (rasterPoint, mapPoint) = tiePoints.head

          val scaleX = (pixel.x - rasterPoint.x) * pixelScales._1
          val scaleY = (pixel.y - rasterPoint.y) * pixelScales._2
          val scaleZ = (pixel.z - rasterPoint.z) * pixelScales._3

          val x = mapPoint.x + scaleX
          val y = mapPoint.y - scaleY
          val z = mapPoint.z + scaleZ

          pixelSampleType match {
            case Some(PixelIsPoint) =>
              // If PixelIsPoint, we have to consider the tie point to be
              // the center of the pixel
              Pixel3D(
                x - (pixelScales._1 * 0.5),
                y + (pixelScales._2 * 0.5),
                z - (pixelScales._3 * 0.5)
              )
            case _ =>
              Pixel3D(x, y, z)
          }
        }

        getExtentFromModelFunction(modelFunc)
      case None =>
        val imageWidth = cols
        val imageLength = rows

        var minX = 0.0
        var minY = 0.0
        var maxX = 0.0
        var maxY = 0.0

        var i = 0
        while(i < 4) {
          val xt = if (i % 2 == 1) imageWidth - 1 else 0
          val yt = if (i >= 2) imageLength - 1 else 0

          val optPixel =
            tiePoints
              .filter { pixel => pixel._1.x == xt && pixel._1.y == yt }
              .map(_._2)
              .headOption

          if (!optPixel.isEmpty) {
            val pixel = optPixel.get
            if (i == 0 || i == 1) maxY = pixel.y
            if (i == 0 || i == 2) minX = pixel.x
            if (i == 1 || i == 3) maxX = pixel.x
            if (i == 2 || i == 3) minY = pixel.y
          }

          i += 1
        }

        // fix an inverted extent, to behave more like GDAL
        Extent(
          math.min(minX, maxX),
          math.min(minY, maxY),
          math.max(minX, maxX),
          math.max(minY, maxY)
        )
    }

  private def getExtentFromModelFunction(func: Pixel3D => Pixel3D) = {
    val modelPixels = getRasterBoundaries.map(func)

    val (x1, y1) = (modelPixels(0).x, modelPixels(0).y)
    val (x2, y2) = (modelPixels(1).x, modelPixels(1).y)

    Extent(math.min(x1, x2),
           math.min(y1, y2),
           math.max(x1, x2),
           math.max(y1, y2))
  }

  def pixelSampleType(): Option[PixelSampleType] =
    geoTiffTags.geoKeyDirectory.flatMap { dir =>
      (dir
        &|-> GeoKeyDirectory._configKeys
        ^|-> ConfigKeys._gtRasterType get) match {
        case Some(v) if v == 1 => Some(PixelIsArea)
        case Some(v) if v == 2 => Some(PixelIsPoint)
        case None => None
      }
    }

  def setGDALNoData(input: String) = (this &|-> TiffTags._geoTiffTags
    ^|-> GeoTiffTags._gdalInternalNoData set (parseGDALNoDataString(input)))

  private lazy val geoTiffCSTags: Option[GeoTiffCSParser] =
    geoTiffTags.geoKeyDirectory.map(GeoTiffCSParser(_))

  def tags: Tags = {
    var (headTags, bandTags) =
      (this &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._metadata get
      ) match {
        case Some(str) => {
          val xml = XML.loadString(str.trim)
          val (metadataXML, bandsMetadataXML) =
            (xml \ "Item")
              .groupBy(_ \ "@sample")
              .partition(_._1.isEmpty)

          val metadata = metadataXML
            .map(_._2)
            .headOption match {
            case Some(ns) => metadataNodeSeqToMap(ns)
            case None => Map[String, String]()
          }

          val bandsMetadataMap = bandsMetadataXML.map { case(key, ns) =>
            (key.toString.toInt, metadataNodeSeqToMap(ns))
          }

          val bandsMetadataBuffer = Array.ofDim[Map[String, String]](bandCount)

          cfor(0)(_ < bandCount, _ + 1) { i =>
            bandsMetadataMap.get(i) match {
              case Some(map) => bandsMetadataBuffer(i) = map
              case None => bandsMetadataBuffer(i) = Map()
            }
          }

          (metadata, bandsMetadataBuffer.toList)
        }
        case None =>
          (Map[String, String](), (0 until bandCount).map { i => Map[String, String]() }.toList)
      }


    // Account for special metadata that should be included as tags

    // Date time tag
    this &|->
      TiffTags._metadataTags ^|->
      MetadataTags._dateTime get match {
        case Some(dateTime) =>
          headTags = headTags + ((Tags.TIFFTAG_DATETIME, dateTime))
        case None =>
      }

    // pixel sample type
    pixelSampleType match {
      case Some(v) if v == PixelIsPoint =>
        headTags = headTags + ((Tags.AREA_OR_POINT, "POINT"))
      case Some(v) if v == PixelIsArea =>
        headTags = headTags + ((Tags.AREA_OR_POINT, "AREA"))
      case _ =>
    }

    Tags(headTags, bandTags)
  }

  private def metadataNodeSeqToMap(ns: NodeSeq): Map[String, String] =
    ns.map(s => ((s \ "@name").text -> s.text)).toMap

  def bandCount: Int =
    this &|->
      TiffTags._basicTags ^|->
      BasicTags._samplesPerPixel get

  def segmentCount: Int =
    if (hasStripStorage) {
      (this
        &|-> TiffTags._basicTags
        ^|-> BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) =>
          stripByteCounts.size
        case None =>
          throw new MalformedGeoTiffException("No StripByteCount information.")
      }
    } else {
      (this
        &|-> TiffTags._tileTags
        ^|-> TileTags._tileOffsets get) match {
        case Some(tileOffsets) =>
          tileOffsets.size
        case None =>
          throw new MalformedGeoTiffException("No TileOffsets information.")
      }
    }
}

object TiffTags {
  def read(path: String): TiffTags =
    read(Filesystem.toMappedByteBuffer(path))

  def read(bytes: Array[Byte]): TiffTags =
    read(ByteBuffer.wrap(bytes))

  def read(byteReader: ByteReader): TiffTags = {
    // set byte ordering
    (byteReader.get.toChar, byteReader.get.toChar) match {
      case ('I', 'I') => byteReader.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteReader.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    byteReader.getChar match {
      case 42 => // Regular GeoTiff
        read(byteReader, byteReader.getInt.toLong)(IntTiffTagOffsetSize)
      case 43 => // BigTiff
        byteReader.position(8)
        read(byteReader, byteReader.getLong)(LongTiffTagOffsetSize)
      case id => // Invalid Tiff identification number
        throw new MalformedGeoTiffException(s"bad identification number (must be 42 or 43, was $id)")
    }
  }

  def read(byteReader: ByteReader, tagsStartPosition: Long)(implicit ttos: TiffTagOffsetSize): TiffTags = {
    val tagCount =
      ttos match {
        case IntTiffTagOffsetSize =>
          byteReader.position(tagsStartPosition.toInt)
          byteReader.getShort
        case LongTiffTagOffsetSize =>
          byteReader.position(tagsStartPosition)
          byteReader.getLong
      }

    // Read the tags.
    var tiffTags = TiffTags()

    // Need to read geo tags last, relies on other tags already being read in.
    var geoTags: Option[TiffTagMetadata] = None

    cfor(0)(_ < tagCount, _ + 1) { i =>
      val tagMetadata =
        ttos match {
          case IntTiffTagOffsetSize =>
            TiffTagMetadata(
              byteReader.getUnsignedShort, // Tag
              byteReader.getUnsignedShort, // Type
              byteReader.getInt,           // Count
              byteReader.getInt            // Offset
            )
          case LongTiffTagOffsetSize =>
            TiffTagMetadata(
              byteReader.getUnsignedShort,
              byteReader.getUnsignedShort,
              byteReader.getLong,
              byteReader.getLong
            )
        }

      if (tagMetadata.tag == codes.TagCodes.GeoKeyDirectoryTag)
        geoTags = Some(tagMetadata)
      else
        tiffTags = readTag(byteReader, tiffTags, tagMetadata)
    }

    geoTags match {
      case Some(t) => tiffTags = readTag(byteReader, tiffTags, t)
      case None =>
    }

    // If it's undefined GDAL interprets the entire TIFF as a single strip
    if(tiffTags.hasStripStorage) {
        val rowsPerStrip =
          (tiffTags
            &|-> TiffTags._basicTags
            ^|-> BasicTags._rowsPerStrip get).toInt
        if (rowsPerStrip < 0) {
          (tiffTags
            &|-> TiffTags._basicTags
            ^|-> BasicTags._rowsPerStrip set(tiffTags.rows))
        } else tiffTags
    } else tiffTags
  }

  private def readTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize): TiffTags = {
    (tagMetadata.tag, tagMetadata.fieldType) match {
      case (ModelPixelScaleTag, _) =>
        readModelPixelScaleTag(byteReader, tiffTags, tagMetadata)
      case (ModelTiePointsTag, _) =>
        readModelTiePointsTag(byteReader, tiffTags, tagMetadata)
      case (GeoKeyDirectoryTag, _) =>
        readGeoKeyDirectoryTag(byteReader, tiffTags, tagMetadata)
      case (_, BytesFieldType) =>
        readBytesTag(byteReader, tiffTags, tagMetadata)
      case (_, AsciisFieldType) =>
        readAsciisTag(byteReader, tiffTags, tagMetadata)
      case (_, ShortsFieldType) =>
        readShortsTag(byteReader, tiffTags, tagMetadata)
      case (_, IntsFieldType) =>
        readIntsTag(byteReader, tiffTags, tagMetadata)
      case (_, FractionalsFieldType) =>
        readFractionalsTag(byteReader, tiffTags, tagMetadata)
      case (_, SignedBytesFieldType) =>
        readSignedBytesTag(byteReader, tiffTags, tagMetadata)
      case (_, UndefinedFieldType) =>
        readUndefinedTag(byteReader, tiffTags, tagMetadata)
      case (_, SignedShortsFieldType) =>
        readSignedShortsTag(byteReader, tiffTags, tagMetadata)
      case (_, SignedIntsFieldType) =>
        readSignedIntsTag(byteReader, tiffTags, tagMetadata)
      case (_, SignedFractionalsFieldType) =>
        readSignedFractionalsTag(byteReader, tiffTags, tagMetadata)
      case (_, FloatsFieldType) =>
        readFloatsTag(byteReader, tiffTags, tagMetadata)
      case (_, DoublesFieldType) =>
        readDoublesTag(byteReader, tiffTags, tagMetadata)
      case (_, LongsFieldType) =>
        readLongsTag(byteReader, tiffTags, tagMetadata)
      case (_, SignedLongsFieldType) =>
        readLongsTag(byteReader, tiffTags, tagMetadata)
      case (_, IFDOffset) =>
        readLongsTag(byteReader, tiffTags, tagMetadata)
      case _ => TiffTags() // skip unsupported tags
    }
  }


  private def readModelPixelScaleTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val oldPos = byteReader.position

    byteReader.position(tagMetadata.offset)

    val scaleX = byteReader.getDouble
    val scaleY = byteReader.getDouble
    val scaleZ = byteReader.getDouble

    byteReader.position(oldPos)

    (tiffTags &|->
      TiffTags._geoTiffTags ^|->
      GeoTiffTags._modelPixelScale set(Some(scaleX, scaleY, scaleZ)))
  }

  private def readModelTiePointsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val oldPos = byteReader.position

    val numberOfPoints = tagMetadata.length / 6

    byteReader.position(tagMetadata.offset)

    val points = Array.ofDim[(Pixel3D, Pixel3D)](numberOfPoints.toInt)
    cfor(0)(_ < numberOfPoints, _ + 1) { i =>
      points(i) =
        (
          Pixel3D(
            byteReader.getDouble,
            byteReader.getDouble,
            byteReader.getDouble
          ),
          Pixel3D(
            byteReader.getDouble,
            byteReader.getDouble,
            byteReader.getDouble
          )
        )
    }

    byteReader.position(oldPos)

    (tiffTags &|->
      TiffTags._geoTiffTags ^|->
      GeoTiffTags._modelTiePoints set(Some(points)))
  }

  private def readGeoKeyDirectoryTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val oldPos = byteReader.position

    byteReader.position(tagMetadata.offset)

    val version = byteReader.getShort
    val keyRevision = byteReader.getShort
    val minorRevision = byteReader.getShort
    val numberOfKeys = byteReader.getShort

    val keyDirectoryMetadata = GeoKeyDirectoryMetadata(version, keyRevision,
      minorRevision, numberOfKeys)

    val geoKeyDirectory = GeoKeyReader.read(byteReader,
      tiffTags, GeoKeyDirectory(count = numberOfKeys))

    byteReader.position(oldPos)

    (tiffTags &|->
      TiffTags._geoTiffTags ^|->
      GeoTiffTags._geoKeyDirectory set(Some(geoKeyDirectory)))
  }

  private def readBytesTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val bytes = byteReader.getByteArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case DotRangeTag => tiffTags &|->
        TiffTags._cmykTags ^|->
        CmykTags._dotRange set(Some(bytes.map(_.toInt)))
      case ExtraSamplesTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._extraSamples set(Some(bytes.map(_.toInt)))
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tag -> bytes.map(_.toLong)))
    }
  }

  private def readAsciisTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize): TiffTags = {

    // Read string, but don't read in trailing 0
    val string =
      byteReader.getString(offset = tagMetadata.offset, length = tagMetadata.length).substring(0, (tagMetadata.length - 1).toInt)

    tagMetadata.tag match {
      case DateTimeTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._dateTime set(Some(string))
      case ImageDescTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._imageDesc set(Some(string))
      case MakerTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._maker set(Some(string))
      case ModelTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._model set(Some(string))
      case SoftwareTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._software set(Some(string))
      case ArtistTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._artist set(Some(string))
      case HostComputerTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._hostComputer set(Some(string))
      case CopyrightTag => tiffTags &|->
        TiffTags._metadataTags ^|->
        MetadataTags._copyright set(Some(string))
      case AsciisTag => tiffTags &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._asciis set(Some(string))
      case MetadataTag => tiffTags &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._metadata set(Some(string))
      case GDALInternalNoDataTag =>
        tiffTags.setGDALNoData(string)
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._asciisMap modify(_ + (tag -> string))
    }
  }

  private def readShortsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val shorts = byteReader.getShortArray(offset = tagMetadata.offset,
      length = tagMetadata.length)

    tagMetadata.tag match {
      case SubfileTypeTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._subfileType set(Some(shorts(0)))
      case ImageWidthTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._imageWidth set(shorts(0))
      case ImageLengthTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._imageLength set(shorts(0))
      case CompressionTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._compression set(shorts(0))
      case PhotometricInterpTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._photometricInterp set(shorts(0))
      case ThresholdingTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._thresholding set(shorts(0))
      case CellWidthTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._cellWidth set(Some(shorts(0)))
      case CellLengthTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._cellLength set(Some(shorts(0)))
      case FillOrderTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._fillOrder set((shorts(0)))
      case OrientationTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._orientation set(shorts(0))
      case SamplesPerPixelTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._samplesPerPixel set(shorts(0))
      case RowsPerStripTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._rowsPerStrip set(shorts(0))
      case PlanarConfigurationTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._planarConfiguration set(Some(shorts(0)))
      case GrayResponseUnitTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._grayResponseUnit set(Some(shorts(0)))
      case ResolutionUnitTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._resolutionUnit set(Some(shorts(0)))
      case PredictorTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._predictor set(Some(shorts(0)))
      case TileWidthTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileWidth set(Some(shorts(0)))
      case TileLengthTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileLength set(Some(shorts(0)))
      case InkSetTag => tiffTags &|->
        TiffTags._cmykTags ^|->
        CmykTags._inkSet set(Some(shorts(0)))
      case NumberOfInksTag => tiffTags &|->
        TiffTags._cmykTags ^|->
        CmykTags._numberOfInks set(Some(shorts(0)))
      case JpegProcTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegProc set(Some(shorts(0)))
      case JpegInterchangeFormatTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegInterchangeFormat set(Some(shorts(0)))
      case JpegInterchangeFormatLengthTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegInterchangeFormatLength set(Some(shorts(0)))
      case JpegRestartIntervalTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegRestartInterval set(Some(shorts(0)))
      case YCbCrPositioningTag => tiffTags &|->
        TiffTags._yCbCrTags ^|->
        YCbCrTags._yCbCrPositioning set(Some(shorts(0)))
      case BitsPerSampleTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._bitsPerSample set(shorts(0))
      case StripOffsetsTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripOffsets set(Some(shorts.map(_.toLong)))
      case StripByteCountsTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts set(Some(shorts.map(_.toLong)))
      case MinSampleValueTag => tiffTags &|->
        TiffTags._dataSampleFormatTags ^|->
        DataSampleFormatTags._minSampleValue set(Some(shorts.map(_.toLong)))
      case MaxSampleValueTag => tiffTags &|->
        TiffTags._dataSampleFormatTags ^|->
        DataSampleFormatTags._maxSampleValue set(Some(shorts.map(_.toLong)))
      case GrayResponseCurveTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._grayResponseCurve set(Some(shorts))
      case PageNumberTag => tiffTags &|->
        TiffTags._documentationTags ^|->
        DocumentationTags._pageNumber set(Some(shorts))
      case TransferFunctionTag => tiffTags &|->
        TiffTags._colimetryTags ^|->
        ColimetryTags._transferFunction set(Some(shorts))
      case ColorMapTag => setColorMap(byteReader, tiffTags, shorts)
      case HalftoneHintsTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._halftoneHints set(Some(shorts))
      case TileByteCountsTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileByteCounts set(Some(shorts.map(_.toLong)))
      case DotRangeTag => tiffTags &|->
        TiffTags._cmykTags ^|->
        CmykTags._dotRange set(Some(shorts))
      case SampleFormatTag => tiffTags &|->
        TiffTags._dataSampleFormatTags ^|->
        DataSampleFormatTags._sampleFormat set(shorts(0))
      case TransferRangeTag => tiffTags &|->
        TiffTags._colimetryTags ^|->
        ColimetryTags._transferRange set(Some(shorts))
      case JpegLosslessPredictorsTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegLosslessPredictors set(Some(shorts))
      case JpegPointTransformsTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegPointTransforms set(Some(shorts))
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tag -> shorts.map(_.toLong)))
    }
  }

  private def setColorMap(byteReader: ByteReader, tiffTags: TiffTags, shorts: Array[Int])(implicit ttos: TiffTagOffsetSize): TiffTags =
    if ((tiffTags &|->
      TiffTags._basicTags ^|->
      BasicTags._photometricInterp get) == 3) {
      // In GDAL world, `divider` ends up being the same as `bitsPerSample`
      // but theoretically it's valid to have color tables that are smaller
      val divider = shorts.length / 3

      val arr = Array.ofDim[(Short, Short, Short)](divider)
      cfor(0)(_ < divider, _ + 1) { i =>
        arr(i) = (
          shorts(i).toShort,
          shorts(i + divider).toShort,
          shorts(i + 2 * divider).toShort
        )
      }

      (tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._colorMap set arr.toSeq)
    } else throw new MalformedGeoTiffException(
      "Colormap without Photometric Interpetation = 3."
    )

  private def readIntsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize): TiffTags = {
    val ints = byteReader.getIntArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case NewSubfileTypeTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._newSubfileType set(Some(ints(0)))
      case ImageWidthTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._imageWidth set(ints(0).toInt)
      case ImageLengthTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._imageLength set(ints(0).toInt)
      case T4OptionsTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._t4Options set(ints(0).toInt)
      case T6OptionsTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._t6Options set(Some(ints(0).toInt))
      case TileWidthTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileWidth set(Some(ints(0)))
      case TileLengthTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileLength set(Some(ints(0)))
      case JpegInterchangeFormatTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegInterchangeFormat set(Some(ints(0)))
      case JpegInterchangeFormatLengthTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegInterchangeFormatLength set(Some(ints(0)))
      case RowsPerStripTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._rowsPerStrip set(ints(0))
      case StripOffsetsTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripOffsets set(Some(ints))
      case StripByteCountsTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts set(Some(ints))
      case FreeOffsetsTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._freeOffsets set(Some(ints))
      case FreeByteCountsTag => tiffTags &|->
        TiffTags._nonBasicTags ^|->
        NonBasicTags._freeByteCounts set(Some(ints))
      case TileOffsetsTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileOffsets set(Some(ints))
      case TileByteCountsTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileByteCounts set(Some(ints))
      case JpegQTablesTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegQTables set(Some(ints))
      case JpegDCTablesTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegDCTables set(Some(ints))
      case JpegACTablesTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegACTables set(Some(ints))
      case ReferenceBlackWhiteTag => tiffTags &|->
        TiffTags._colimetryTags ^|->
        ColimetryTags._referenceBlackWhite set(Some(ints))
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tag -> ints.map(_.toLong)))
    }
  }

  private def readLongsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val longs = byteReader.getLongArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case StripOffsetsTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripOffsets set(Some(longs))
      case StripByteCountsTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts set(Some(longs))
      case TileOffsetsTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileOffsets set(Some(longs))
      case TileByteCountsTag => tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileByteCounts set(Some(longs))
    }
  }

  private def readFractionalsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val fractionals = byteReader.getFractionalArray(offset = tagMetadata.offset,
      length = tagMetadata.length)

    tagMetadata.tag match {
      case XResolutionTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._xResolution set(Some(fractionals(0)))
      case YResolutionTag => tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._yResolution set(Some(fractionals(0)))
      case XPositionTag => tiffTags &|->
        TiffTags._documentationTags ^|->
        DocumentationTags._xPositions set(Some(fractionals))
      case YPositionTag => tiffTags &|->
        TiffTags._documentationTags ^|->
        DocumentationTags._yPositions set(Some(fractionals))
      case WhitePointTag => tiffTags &|->
        TiffTags._colimetryTags ^|->
        ColimetryTags._whitePoints set(Some(fractionals))
      case PrimaryChromaticitiesTag => tiffTags &|->
        TiffTags._colimetryTags ^|->
        ColimetryTags._primaryChromaticities set(Some(fractionals))
      case YCbCrCoefficientsTag => tiffTags &|->
        TiffTags._yCbCrTags ^|->
        YCbCrTags._yCbCrCoefficients set(Some(fractionals))
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._fractionalsMap modify(
          _ + (tag -> fractionals)
        )
    }
  }

  private def readSignedBytesTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val bytes = byteReader.getSignedByteArray(offset = tagMetadata.offset, length = tagMetadata.length)

    (tiffTags &|->
      TiffTags._nonStandardizedTags ^|->
      NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> bytes.map(_.toLong))))
  }

  private def readUndefinedTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val bytes = byteReader.getSignedByteArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case JpegTablesTag => tiffTags &|->
        TiffTags._jpegTags ^|->
        JpegTags._jpegTables set(Some(bytes))
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._undefinedMap modify(_ + (tag -> bytes))
    }
  }

  private def readSignedShortsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val shorts = byteReader.getSignedShortArray(offset = tagMetadata.offset, length = tagMetadata.length)

    (tiffTags &|->
      TiffTags._nonStandardizedTags ^|->
      NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> shorts.map(_.toLong))))
  }

  private def readSignedIntsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val ints = byteReader.getSignedIntArray(offset = tagMetadata.offset, length = tagMetadata.offset)

    (tiffTags &|->
      TiffTags._nonStandardizedTags ^|->
      NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> ints.map(_.toLong))))
  }

  private def readSignedFractionalsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val fractionals = byteReader.getSignedFractionalArray(tagMetadata.offset, length = tagMetadata.length)

    (tiffTags &|->
      TiffTags._nonStandardizedTags ^|->
      NonStandardizedTags._fractionalsMap modify(
        _ + (tagMetadata.tag -> fractionals.map(x => (x._1.toLong, x._2.toLong)))
      ))
  }

  private def readFloatsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val floats = byteReader.getFloatArray(offset = tagMetadata.offset, length = tagMetadata.length)

    (tiffTags &|->
      TiffTags._nonStandardizedTags ^|->
      NonStandardizedTags._doublesMap modify(
        _ + (tagMetadata.tag -> floats.map(_.toDouble))
      ))
  }

  private def readDoublesTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val doubles = byteReader.getDoubleArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case ModelTransformationTag =>
        if (doubles.size != 16)
          throw new MalformedGeoTiffException("bad model tranformations")
        else {
          val matrix = Array(
            Array(doubles(0), doubles(1), doubles(2), doubles(3)),
            Array(doubles(4), doubles(5), doubles(6), doubles(7)),
            Array(doubles(8), doubles(9), doubles(10), doubles(11)),
            Array(doubles(12), doubles(13), doubles(14), doubles(15))
          )

          (tiffTags &|->
            TiffTags._geoTiffTags ^|->
            GeoTiffTags._modelTransformation set(Some(matrix)))
        }
      case DoublesTag => tiffTags &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._doubles set(Some(doubles))
      case tag => tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._doublesMap modify(_ + (tag -> doubles))
    }
  }

  implicit val tiffTagsEncoder: Encoder[TiffTags] = deriveEncoder[TiffTags]
  implicit val tiffTagsDecoder: Decoder[TiffTags] = deriveDecoder[TiffTags]
}