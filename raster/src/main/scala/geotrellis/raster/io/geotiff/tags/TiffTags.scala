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

import io.circe.*
import io.circe.generic.semiauto.*
import geotrellis.proj4.{CRS, LatLng}
import geotrellis.vector.Extent
import geotrellis.raster.*
import geotrellis.raster.io.geotiff.*
import geotrellis.raster.io.geotiff.reader.{MalformedGeoTiffException, GeoTiffCSParser}
import geotrellis.raster.io.geotiff.tags.*
import geotrellis.raster.io.geotiff.tags.codes.*
import geotrellis.raster.io.geotiff.util.*
import geotrellis.util.{ByteReader, Filesystem}
import java.nio.{ByteBuffer, ByteOrder}
import ModelTypes.*
import monocle.syntax.all.*
import ProjectionTypesMap.UserDefinedProjectionType
import spire.syntax.cfor.*
import TagCodes.*
import TiffFieldType.*
import xml.*

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
    if (this.hasStripStorage())
      (this.focus(_.basicTags.stripOffsets).get).get
    else
      (this.focus(_.tileTags.tileOffsets).get).get

  def segmentByteCounts: Array[Long] =
    if (this.hasStripStorage())
      (this.focus(_.basicTags.stripByteCounts).get).get
    else
      (this.focus(_.tileTags.tileByteCounts).get).get


  def storageMethod: StorageMethod =
    if(hasStripStorage()) {
      val rowsPerStrip: Int =
        (this.focus(_.basicTags.rowsPerStrip).get).toInt

      Striped(rowsPerStrip)
    } else {
      val blockCols =
        (this.focus(_.tileTags.tileWidth).get).get.toInt

      val blockRows =
        (this.focus(_.tileTags.tileLength).get).get.toInt

      Tiled(blockCols, blockRows)
    }

  def geoTiffSegmentLayout: GeoTiffSegmentLayout =
    GeoTiffSegmentLayout(this.cols, this.rows, this.storageMethod, this.interleaveMethod(), this.bandType)

  def cellSize =
    CellSize(this.extent.width / this.cols, this.extent.height / this.rows)

  def compression =
    (this.focus(_.basicTags.compression).get)

  def hasStripStorage(): Boolean =
    (this.focus(_.tileTags.tileWidth).get).isEmpty

  def interleaveMethod(): InterleaveMethod =
    (this.focus(_.nonBasicTags.planarConfiguration).get) match {
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
    interleaveMethod() == PixelInterleave

  def rowsInStrip(index: Int): Option[Long] =
    if (hasStripStorage()) {
      (this.focus(_.basicTags.stripByteCounts).get) match {
        case Some(stripByteCounts) => {
          val rowsPerStrip = (this.focus(_.basicTags.rowsPerStrip).get)
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
    if (hasStripStorage())
      rowsInStrip(index).get.toInt
    else
      (this.focus(_.tileTags.tileLength).get).get.toInt

  def bitsPerPixel(): Int =
    bitsPerSample * bandCount

  def bytesPerPixel: Int =
    (this.bitsPerPixel() + 7) / 8

  def bitsPerSample: Int =
    (this.focus(_.basicTags.bitsPerSample).get)

  def imageSegmentByteSize(index: Int): Long =
    {(imageSegmentBitsSize(index) + 7) / 8 }

  def imageSegmentBitsSize(index: Int): Long =
    if (hasStripStorage()) {
      val c = {
        // For 1 bit rasters, take into account
        // that the rows are padded with extra bits to make
        // up the last byte.
        if(bitsPerPixel() == 1) {
          val m = (cols + 7) / 8
          8 * m
        } else {
          cols
        }
      }

      (rowsInStrip(index).get * c * bitsPerPixel()) / bandCount
    }
    else {
      // We don't need the same check for 1 bit rasters as above,
      // because according the the TIFF 6.0 Spec, "TileWidth must be a multiple of 16".
      (
        (this.focus(_.tileTags.tileWidth).get),
        (this.focus(_.tileTags.tileLength).get)
      ) match {
        case (Some(tileWidth), Some(tileHeight)) =>
          (bitsPerPixel() * tileWidth * tileHeight) / bandCount
        case _ =>
          throw new MalformedGeoTiffException("Cannot find TileWidth and TileLength tags for tiled GeoTiff.")
      }
    }

  def rowSize: Int =
    if (hasStripStorage()) cols
    else (this.focus(_.tileTags.tileWidth).get).get.toInt

  def cols = (this.focus(_.basicTags.imageWidth).get)
  def rows = (this.focus(_.basicTags.imageLength).get)

  def extent: Extent =
    this.focus(_.geoTiffTags.modelTransformation).get match {
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
        this.focus(_.geoTiffTags.modelTiePoints).get match {
          case Some(tiePoints) if (!tiePoints.isEmpty) =>
            tiePointsModelSpace(
              tiePoints,
              this.focus(_.geoTiffTags.modelPixelScale).get
            )
          case _ =>
            Extent(0, 0, cols, rows)
        }
    }

  def bandType: BandType = {
    val sampleFormat =
      (this.focus(_.dataSampleFormatTags.sampleFormat).get)

    BandType(bitsPerSample, sampleFormat)
  }

  def noDataValue =
    (this.focus(_.geoTiffTags.gdalInternalNoData).get)

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

          pixelSampleType() match {
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
      (dir.focus(_.configKeys.gtRasterType).get) match {
        case Some(1) => Some(PixelIsArea)
        case Some(2) => Some(PixelIsPoint)
        case _       => None
      }
    }

  def setGDALNoData(input: String) = (this.focus(_.geoTiffTags.gdalInternalNoData).replace(parseGDALNoDataString(input)))

  private lazy val geoTiffCSTags: Option[GeoTiffCSParser] =
    geoTiffTags.geoKeyDirectory.map(GeoTiffCSParser(_))

  def tags: Tags = {
    var (headTags, bandTags) =
      this.focus(_.geoTiffTags.metadata).get match {
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
    this.focus(_.metadataTags.dateTime).get match {
        case Some(dateTime) =>
          headTags = headTags + ((Tags.TIFFTAG_DATETIME, dateTime))
        case None =>
      }

    // pixel sample type
    pixelSampleType() match {
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
    this.focus(_.basicTags.samplesPerPixel).get

  def segmentCount: Int =
    if (hasStripStorage()) {
      (this.focus(_.basicTags.stripByteCounts).get) match {
        case Some(stripByteCounts) =>
          stripByteCounts.size
        case None =>
          throw new MalformedGeoTiffException("No StripByteCount information.")
      }
    } else {
      (this.focus(_.tileTags.tileOffsets).get) match {
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
    val tagCount: Long =
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
    if(tiffTags.hasStripStorage()) {
        val rowsPerStrip =
          (tiffTags.focus(_.basicTags.rowsPerStrip).get).toInt
        if (rowsPerStrip < 0) {
          (tiffTags.focus(_.basicTags.rowsPerStrip).replace(tiffTags.rows))
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
      case _ => tiffTags // skip unsupported tags
    }
  }


  private def readModelPixelScaleTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val oldPos = byteReader.position()

    byteReader.position(tagMetadata.offset)

    val scaleX = byteReader.getDouble
    val scaleY = byteReader.getDouble
    val scaleZ = byteReader.getDouble

    byteReader.position(oldPos)

    (tiffTags.focus(_.geoTiffTags.modelPixelScale).replace(Some(scaleX, scaleY, scaleZ)))
  }

  private def readModelTiePointsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val oldPos = byteReader.position()

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

    (tiffTags.focus(_.geoTiffTags.modelTiePoints).replace(Some(points)))
  }

  private def readGeoKeyDirectoryTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val oldPos = byteReader.position()

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

    (tiffTags.focus(_.geoTiffTags.geoKeyDirectory).replace(Some(geoKeyDirectory)))
  }

  private def readBytesTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {

    val bytes = byteReader.getByteArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case DotRangeTag => tiffTags.focus(_.cmykTags.dotRange).replace(Some(bytes.map(_.toInt)))
      case ExtraSamplesTag => tiffTags.focus(_.nonBasicTags.extraSamples).replace(Some(bytes.map(_.toInt)))
      case tag => tiffTags.focus(_.nonStandardizedTags.longsMap).modify(_ + (tag -> bytes.map(_.toLong)))
    }
  }

  private def readAsciisTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize): TiffTags = {

    // Read string, but don't read in trailing 0
    val string =
      byteReader.getString(offset = tagMetadata.offset, length = tagMetadata.length).substring(0, (tagMetadata.length - 1).toInt)

    tagMetadata.tag match {
      case DateTimeTag => tiffTags.focus(_.metadataTags.dateTime).replace(Some(string))
      case ImageDescTag => tiffTags.focus(_.metadataTags.imageDesc).replace(Some(string))
      case MakerTag => tiffTags.focus(_.metadataTags.maker).replace(Some(string))
      case ModelTag => tiffTags.focus(_.metadataTags.model).replace(Some(string))
      case SoftwareTag => tiffTags.focus(_.metadataTags.software).replace(Some(string))
      case ArtistTag => tiffTags.focus(_.metadataTags.artist).replace(Some(string))
      case HostComputerTag => tiffTags.focus(_.metadataTags.hostComputer).replace(Some(string))
      case CopyrightTag => tiffTags.focus(_.metadataTags.copyright).replace(Some(string))
      case AsciisTag => tiffTags.focus(_.geoTiffTags.asciis).replace(Some(string))
      case MetadataTag => tiffTags.focus(_.geoTiffTags.metadata).replace(Some(string))
      case GDALInternalNoDataTag =>
        tiffTags.setGDALNoData(string)
      case tag => tiffTags.focus(_.nonStandardizedTags.asciisMap).modify(_ + (tag -> string))
    }
  }

  private def readShortsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val shorts = byteReader.getShortArray(offset = tagMetadata.offset,
      length = tagMetadata.length)

    tagMetadata.tag match {
      case SubfileTypeTag => tiffTags.focus(_.nonBasicTags.subfileType).replace(Some(shorts(0)))
      case ImageWidthTag => tiffTags.focus(_.basicTags.imageWidth).replace(shorts(0))
      case ImageLengthTag => tiffTags.focus(_.basicTags.imageLength).replace(shorts(0))
      case CompressionTag => tiffTags.focus(_.basicTags.compression).replace(shorts(0))
      case PhotometricInterpTag => tiffTags.focus(_.basicTags.photometricInterp).replace(shorts(0))
      case ThresholdingTag => tiffTags.focus(_.nonBasicTags.thresholding).replace(shorts(0))
      case CellWidthTag => tiffTags.focus(_.nonBasicTags.cellWidth).replace(Some(shorts(0)))
      case CellLengthTag => tiffTags.focus(_.nonBasicTags.cellLength).replace(Some(shorts(0)))
      case FillOrderTag => tiffTags.focus(_.nonBasicTags.fillOrder).replace((shorts(0)))
      case OrientationTag => tiffTags.focus(_.nonBasicTags.orientation).replace(shorts(0))
      case SamplesPerPixelTag => tiffTags.focus(_.basicTags.samplesPerPixel).replace(shorts(0))
      case RowsPerStripTag => tiffTags.focus(_.basicTags.rowsPerStrip).replace(shorts(0))
      case PlanarConfigurationTag => tiffTags.focus(_.nonBasicTags.planarConfiguration).replace(Some(shorts(0)))
      case GrayResponseUnitTag => tiffTags.focus(_.nonBasicTags.grayResponseUnit).replace(Some(shorts(0)))
      case ResolutionUnitTag => tiffTags.focus(_.basicTags.resolutionUnit).replace(Some(shorts(0)))
      case PredictorTag => tiffTags.focus(_.nonBasicTags.predictor).replace(Some(shorts(0)))
      case TileWidthTag => tiffTags.focus(_.tileTags.tileWidth).replace(Some(shorts(0)))
      case TileLengthTag => tiffTags.focus(_.tileTags.tileLength).replace(Some(shorts(0)))
      case InkSetTag => tiffTags.focus(_.cmykTags.inkSet).replace(Some(shorts(0)))
      case NumberOfInksTag => tiffTags.focus(_.cmykTags.numberOfInks).replace(Some(shorts(0)))
      case JpegProcTag => tiffTags.focus(_.jpegTags.jpegProc).replace(Some(shorts(0)))
      case JpegInterchangeFormatTag => tiffTags.focus(_.jpegTags.jpegInterchangeFormat).replace(Some(shorts(0)))
      case JpegInterchangeFormatLengthTag =>
        tiffTags.focus(_.jpegTags.jpegInterchangeFormatLength).replace(Some(shorts(0)))
      case JpegRestartIntervalTag => tiffTags.focus(_.jpegTags.jpegRestartInterval).replace(Some(shorts(0)))
      case YCbCrPositioningTag => tiffTags.focus(_.yCbCrTags.yCbCrPositioning).replace(Some(shorts(0)))
      case BitsPerSampleTag => tiffTags.focus(_.basicTags.bitsPerSample).replace(shorts(0))
      case StripOffsetsTag => tiffTags.focus(_.basicTags.stripOffsets).replace(Some(shorts.map(_.toLong)))
      case StripByteCountsTag => tiffTags.focus(_.basicTags.stripByteCounts).replace(Some(shorts.map(_.toLong)))
      case MinSampleValueTag =>
        tiffTags.focus(_.dataSampleFormatTags.minSampleValue).replace(Some(shorts.map(_.toLong)))
      case MaxSampleValueTag =>
        tiffTags.focus(_.dataSampleFormatTags.maxSampleValue).replace(Some(shorts.map(_.toLong)))
      case GrayResponseCurveTag => tiffTags.focus(_.nonBasicTags.grayResponseCurve).replace(Some(shorts))
      case PageNumberTag => tiffTags.focus(_.documentationTags.pageNumber).replace(Some(shorts))
      case TransferFunctionTag => tiffTags.focus(_.colimetryTags.transferFunction).replace(Some(shorts))
      case ColorMapTag => setColorMap(byteReader, tiffTags, shorts)
      case HalftoneHintsTag => tiffTags.focus(_.nonBasicTags.halftoneHints).replace(Some(shorts))
      case TileByteCountsTag => tiffTags.focus(_.tileTags.tileByteCounts).replace(Some(shorts.map(_.toLong)))
      case DotRangeTag => tiffTags.focus(_.cmykTags.dotRange).replace(Some(shorts))
      case SampleFormatTag => tiffTags.focus(_.dataSampleFormatTags.sampleFormat).replace(shorts(0))
      case TransferRangeTag => tiffTags.focus(_.colimetryTags.transferRange).replace(Some(shorts))
      case JpegLosslessPredictorsTag => tiffTags.focus(_.jpegTags.jpegLosslessPredictors).replace(Some(shorts))
      case JpegPointTransformsTag => tiffTags.focus(_.jpegTags.jpegPointTransforms).replace(Some(shorts))
      case ExtraSamplesTag => tiffTags.focus(_.nonBasicTags.extraSamples).replace(Some(shorts))
      case tag => tiffTags.focus(_.nonStandardizedTags.longsMap).modify(_ + (tag -> shorts.map(_.toLong)))
    }
  }

  private def setColorMap(byteReader: ByteReader, tiffTags: TiffTags, shorts: Array[Int])(implicit ttos: TiffTagOffsetSize): TiffTags =
    if ((tiffTags.focus(_.basicTags.photometricInterp).get) == 3) {
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

      tiffTags.focus(_.basicTags.colorMap).replace(arr.toSeq)
    } else throw new MalformedGeoTiffException(
      "Colormap without Photometric Interpetation = 3."
    )

  private def readIntsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize): TiffTags = {
    val ints = byteReader.getIntArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case NewSubfileTypeTag => tiffTags.focus(_.nonBasicTags.newSubfileType).replace(Some(ints(0)))
      case ImageWidthTag => tiffTags.focus(_.basicTags.imageWidth).replace(ints(0).toInt)
      case ImageLengthTag => tiffTags.focus(_.basicTags.imageLength).replace(ints(0).toInt)
      case T4OptionsTag => tiffTags.focus(_.nonBasicTags.t4Options).replace(ints(0).toInt)
      case T6OptionsTag => tiffTags.focus(_.nonBasicTags.t6Options).replace(Some(ints(0).toInt))
      case TileWidthTag => tiffTags.focus(_.tileTags.tileWidth).replace(Some(ints(0)))
      case TileLengthTag => tiffTags.focus(_.tileTags.tileLength).replace(Some(ints(0)))
      case JpegInterchangeFormatTag => tiffTags.focus(_.jpegTags.jpegInterchangeFormat).replace(Some(ints(0)))
      case JpegInterchangeFormatLengthTag =>
        tiffTags.focus(_.jpegTags.jpegInterchangeFormatLength).replace(Some(ints(0)))
      case RowsPerStripTag => tiffTags.focus(_.basicTags.rowsPerStrip).replace(ints(0))
      case StripOffsetsTag => tiffTags.focus(_.basicTags.stripOffsets).replace(Some(ints))
      case StripByteCountsTag => tiffTags.focus(_.basicTags.stripByteCounts).replace(Some(ints))
      case FreeOffsetsTag => tiffTags.focus(_.nonBasicTags.freeOffsets).replace(Some(ints))
      case FreeByteCountsTag => tiffTags.focus(_.nonBasicTags.freeByteCounts).replace(Some(ints))
      case TileOffsetsTag => tiffTags.focus(_.tileTags.tileOffsets).replace(Some(ints))
      case TileByteCountsTag => tiffTags.focus(_.tileTags.tileByteCounts).replace(Some(ints))
      case JpegQTablesTag => tiffTags.focus(_.jpegTags.jpegQTables).replace(Some(ints))
      case JpegDCTablesTag => tiffTags.focus(_.jpegTags.jpegDCTables).replace(Some(ints))
      case JpegACTablesTag => tiffTags.focus(_.jpegTags.jpegACTables).replace(Some(ints))
      case ReferenceBlackWhiteTag => tiffTags.focus(_.colimetryTags.referenceBlackWhite).replace(Some(ints))
      case tag => tiffTags.focus(_.nonStandardizedTags.longsMap).modify(_ + (tag -> ints.map(_.toLong)))
    }
  }

  private def readLongsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val longs = byteReader.getLongArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case StripOffsetsTag => tiffTags.focus(_.basicTags.stripOffsets).replace(Some(longs))
      case StripByteCountsTag => tiffTags.focus(_.basicTags.stripByteCounts).replace(Some(longs))
      case TileOffsetsTag => tiffTags.focus(_.tileTags.tileOffsets).replace(Some(longs))
      case TileByteCountsTag => tiffTags.focus(_.tileTags.tileByteCounts).replace(Some(longs))
    }
  }

  private def readFractionalsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val fractionals = byteReader.getFractionalArray(offset = tagMetadata.offset,
      length = tagMetadata.length)

    tagMetadata.tag match {
      case XResolutionTag => tiffTags.focus(_.basicTags.xResolution).replace(Some(fractionals(0)))
      case YResolutionTag => tiffTags.focus(_.basicTags.yResolution).replace(Some(fractionals(0)))
      case XPositionTag => tiffTags.focus(_.documentationTags.xPositions).replace(Some(fractionals))
      case YPositionTag => tiffTags.focus(_.documentationTags.yPositions).replace(Some(fractionals))
      case WhitePointTag => tiffTags.focus(_.colimetryTags.whitePoints).replace(Some(fractionals))
      case PrimaryChromaticitiesTag => tiffTags.focus(_.colimetryTags.primaryChromaticities).replace(Some(fractionals))
      case YCbCrCoefficientsTag => tiffTags.focus(_.yCbCrTags.yCbCrCoefficients).replace(Some(fractionals))
      case tag => tiffTags.focus(_.nonStandardizedTags.fractionalsMap).modify(
          _ + (tag -> fractionals)
        )
    }
  }

  private def readSignedBytesTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val bytes = byteReader.getSignedByteArray(offset = tagMetadata.offset, length = tagMetadata.length)

    (tiffTags.focus(_.nonStandardizedTags.longsMap).modify(_ + (tagMetadata.tag -> bytes.map(_.toLong))))
  }

  private def readUndefinedTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val bytes = byteReader.getSignedByteArray(offset = tagMetadata.offset, length = tagMetadata.length)

    tagMetadata.tag match {
      case JpegTablesTag => tiffTags.focus(_.jpegTags.jpegTables).replace(Some(bytes))
      case tag => tiffTags.focus(_.nonStandardizedTags.undefinedMap).modify(_ + (tag -> bytes))
    }
  }

  private def readSignedShortsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val shorts = byteReader.getSignedShortArray(offset = tagMetadata.offset, length = tagMetadata.length)

    (tiffTags.focus(_.nonStandardizedTags.longsMap).modify(_ + (tagMetadata.tag -> shorts.map(_.toLong))))
  }

  private def readSignedIntsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val ints = byteReader.getSignedIntArray(offset = tagMetadata.offset, length = tagMetadata.offset)

    (tiffTags.focus(_.nonStandardizedTags.longsMap).modify(_ + (tagMetadata.tag -> ints.map(_.toLong))))
  }

  private def readSignedFractionalsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val fractionals = byteReader.getSignedFractionalArray(tagMetadata.offset, length = tagMetadata.length)

    (tiffTags.focus(_.nonStandardizedTags.fractionalsMap).modify(
        _ + (tagMetadata.tag -> fractionals.map(x => (x._1.toLong, x._2.toLong)))
      ))
  }

  private def readFloatsTag(byteReader: ByteReader, tiffTags: TiffTags, tagMetadata: TiffTagMetadata)(implicit ttos: TiffTagOffsetSize) = {
    val floats = byteReader.getFloatArray(offset = tagMetadata.offset, length = tagMetadata.length)

    (tiffTags.focus(_.nonStandardizedTags.doublesMap).modify(
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

          (tiffTags.focus(_.geoTiffTags.modelTransformation).replace(Some(matrix)))
        }
      case DoublesTag => tiffTags.focus(_.geoTiffTags.doubles).replace(Some(doubles))
      case tag => tiffTags.focus(_.nonStandardizedTags.doublesMap).modify(_ + (tag -> doubles))
    }
  }

  implicit val tiffTagsEncoder: Encoder[TiffTags] = deriveEncoder[TiffTags]
  implicit val tiffTagsDecoder: Decoder[TiffTags] = deriveDecoder[TiffTags]
}