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

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.codes._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.util._
import geotrellis.util.ByteReader
import ModelTypes._
import ProjectionTypesMap.UserDefinedProjectionType

import geotrellis.vector.Extent

import geotrellis.proj4.CRS
import geotrellis.proj4.LatLng

import collection.immutable.Map

import xml._

import monocle.syntax.apply._
import monocle.macros.Lenses

import spire.syntax.cfor._

object TiffTags {
  def apply(path: String): TiffTags =
    TiffTagsReader.read(path)

  def apply(byteReader: ByteReader): TiffTags =
    TiffTagsReader.read(byteReader)
}

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

        Extent(minX, minY, maxX, maxY)
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
