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
import geotrellis.raster.io.arg.ArgWriter
import geotrellis.raster.io.geotiff.reader.CommonPublicValues._

import geotrellis.vector.Extent

import geotrellis.proj4.CRS
import geotrellis.proj4.LatLng

import collection.immutable.{HashMap, Map}

import collection.mutable.ListBuffer

import xml._

import monocle.syntax._
import monocle.macros.Lenses

import spire.syntax.cfor._
@Lenses("_")
private[reader]
case class ImageDirectory(
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
  imageBytes: Array[Byte] = Array[Byte]()
) {

  val compression = (this &|->
    ImageDirectory._basicTags ^|->
    BasicTags._compression get)

  def hasStripStorage(): Boolean = (this &|->
    ImageDirectory._tileTags ^|->
    TileTags._tileWidth get).isEmpty

  def tileBitsSize(): Option[Long] =
    (
      (this &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileWidth get),
      (this &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileLength get)
    ) match {
      case (Some(tileWidth), Some(tileLength)) =>
        Some(bitsPerPixel * tileWidth * tileLength)
      case _ => None
    }

  def rowsInStrip(index: Int): Option[Long] = if (hasStripStorage) {
    (this &|->
      ImageDirectory._basicTags ^|->
      BasicTags._stripByteCounts get) match {
      case Some(stripByteCounts) => {
        val rowsPerStrip = (this &|->
          ImageDirectory._basicTags ^|->
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
      case None => throw new MalformedGeoTiffException("bad rows/tile structure")
    }
  } else None

  def rowsInSegment(index: Int): Int =
    if (hasStripStorage)
      rowsInStrip(index).get.toInt
    else
      (this &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileLength get).get.toInt

  def bitsPerPixel(): Int = (this &|->
    ImageDirectory._basicTags ^|->
    BasicTags._bitsPerSample get) match {
    case Some(v) => v.sum
    case None => (this &|->
        ImageDirectory._basicTags ^|->
        BasicTags._samplesPerPixel get)
  }

  def imageSegmentByteSize(index: Option[Int] = None): Long =
    (imageSegmentBitsSize(index) + 7) / 8

  def imageSegmentBitsSize(index: Option[Int] = None): Long =
    if (hasStripStorage && !index.isEmpty)
      rowsInStrip(index.get).get * cols * bitsPerPixel
    else tileBitsSize.get * bitsPerPixel

  def rowSize: Int =
    if (hasStripStorage) cols
    else (this &|-> ImageDirectory._tileTags ^|-> TileTags._tileWidth get).get.toInt

  lazy val getRasterBoundaries: Array[Pixel3D] = {
    val imageWidth = cols
    val imageLength = rows

    Array(
      Pixel3D(0, imageLength, 0),
      Pixel3D(imageWidth, 0, 0)
    )
  }

  lazy val cols = (this &|-> ImageDirectory._basicTags ^|-> BasicTags._imageWidth get)
  lazy val rows = (this &|-> ImageDirectory._basicTags ^|-> BasicTags._imageLength get)

  lazy val metaData: GeoTiffMetaData = {
    val extent: Extent = (this &|-> ImageDirectory._geoTiffTags
      ^|-> GeoTiffTags._modelTransformation get) match {
      case Some(trans) if (trans.validateAsMatrix && trans.size == 4
          && trans(0).size == 4) => transformationModelSpace(trans)
      case _ => (this &|-> ImageDirectory._geoTiffTags
          ^|-> GeoTiffTags._modelTiePoints get) match {
        case Some(tiePoints) if (!tiePoints.isEmpty) =>
          tiePointsModelSpace(
            tiePoints,
            (this &|-> ImageDirectory._geoTiffTags
              ^|-> GeoTiffTags._modelPixelScale get)
          )
        case _ => Extent(0, 0, cols, rows)
      }
    }

    lazy val crs: CRS = proj4String match {
      case Some(s) => CRS.fromString(s)
      case None => LatLng
    }


    lazy val cellType: CellType =
      ((this &|-> ImageDirectory._basicTags
        ^|-> BasicTags._bitsPerSample get),
        (this &|-> ImageDirectory._dataSampleFormatTags
          ^|-> DataSampleFormatTags._sampleFormat get)) match {
        case (Some(bitsPerSampleArray), sampleFormatArray)
            if (bitsPerSampleArray.size > 0 && sampleFormatArray.size > 0) => {
              val bitsPerSample = bitsPerSampleArray(0)

              val sampleFormat = sampleFormatArray(0)

              import SampleFormat._

              if (bitsPerSample == 1) TypeBit
              else if (bitsPerSample <= 8) TypeByte
              else if (bitsPerSample <= 16) TypeShort
              else if (bitsPerSample == 32 && sampleFormat == UnsignedInt
                || sampleFormat == SignedInt) TypeInt
              else if (bitsPerSample == 32 && sampleFormat == FloatingPoint) TypeFloat
              else if (bitsPerSample == 64 && sampleFormat == FloatingPoint) TypeDouble
              else throw new MalformedGeoTiffException(
                s"bad/unsupported bitspersample or sampleformat: $bitsPerSample or $sampleFormat"
              )
            }

        case _ =>
          throw new MalformedGeoTiffException("no bitsPerSample values!")
      }

    GeoTiffMetaData(
      RasterExtent(extent, cols, rows),
      crs,
      cellType
    )
  }

  lazy val geoKeyDirectory = geoTiffTags.geoKeyDirectory.getOrElse {
    throw new IllegalAccessException("no geo key directory present")
  }

  private def transformationModelSpace(modelTransformation: Array[Array[Double]]) = {
    def matrixMult(pixel: Pixel3D) = Pixel3D.fromArray((modelTransformation *
      Array(Array(pixel.x, pixel.y, pixel.z, 1))).flatten.take(3))

    getExtentFromModelFunction(matrixMult)
  }

  private def tiePointsModelSpace(tiePoints: Array[(Pixel3D, Pixel3D)],
    pixelScaleOption: Option[(Double, Double, Double)]) =
    pixelScaleOption match {
      case Some(pixelScales) => {
        def modelFunc(pixel: Pixel3D) = {
          val (first, second) = tiePoints.head

          val scaleX = (pixel.x - first.x) * pixelScales._1
          val scaleY = (pixel.y - first.y) * pixelScales._2
          val scaleZ = (pixel.z - first.z) * pixelScales._3

          Pixel3D(scaleX + second.x, second.y - scaleY, scaleZ + second.z)
        }

        getExtentFromModelFunction(modelFunc)
      }
      case None => {
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

          val optPixel = tiePoints.filter(pixel => pixel._1.x == xt &&
            pixel._1.y == yt).map(_._2).headOption

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
    }

  private def getExtentFromModelFunction(func: Pixel3D => Pixel3D) = {
    val modelPixels = getRasterBoundaries.map(func)

    val (minX, minY) = (modelPixels(0).x, modelPixels(0).y)
    val (maxX, maxY) = (modelPixels(1).x, modelPixels(1).y)

    Extent(minX, minY, maxX, maxY)
  }

  def hasPixelArea(): Boolean =
    (geoKeyDirectory &|->
      GeoKeyDirectory._configKeys ^|->
      ConfigKeys._gtRasterType get) match {
      case Some(UndefinedCPV) => throw new MalformedGeoTiffException(
        "the raster type must be present."
      )
      case Some(UserDefinedCPV) => throw new GeoTiffReaderLimitationException(
        "this reader doesn't support user defined raster types."
      )
      case Some(v) => v == 1
      case None => true
    }

  def setGDALNoData(input: String) = (this &|-> ImageDirectory._geoTiffTags
    ^|-> GeoTiffTags._gdalInternalNoData set (parseGDALNoDataString(input)))

  lazy val proj4String: Option[String] = try {
    GeoTiffCSParser(this).getProj4String
  } catch {
    case e: Exception => None
  }

  lazy val (tags, bandTags): (Map[String, String], Seq[Map[String, String]]) =
    (
      (this &|->
        ImageDirectory._basicTags ^|->
        BasicTags._samplesPerPixel get),
      (this &|->
        ImageDirectory._geoTiffTags ^|->
        GeoTiffTags._metadata get)
    ) match {
      case (numberOfBands, Some(str)) => {
        val xml = XML.loadString(str.trim)
        val (metadataXML, bandsMetadataXML) = (xml \ "Item")
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

        val bandsMetadataBuffer = Array.ofDim[Map[String, String]](numberOfBands)

        cfor(0)(_ < numberOfBands, _ + 1) { i =>
          bandsMetadataMap.get(i) match {
            case Some(map) => bandsMetadataBuffer(i) = map
            case None => bandsMetadataBuffer(i) = Map()
          }
        }

        (metadata, bandsMetadataBuffer)
      }
      case (numberOfBands, None) => (
        Map(),
        Array.ofDim[Map[String, String]](numberOfBands)
      )
    }

  private def metadataNodeSeqToMap(ns: NodeSeq): Map[String, String] =
    ns.map(s => ((s \ "@name").text -> s.text)).toMap

  private def toTile(bytes: Array[Byte]): ArrayTile =
    (this &|-> ImageDirectory._geoTiffTags
      ^|-> GeoTiffTags._gdalInternalNoData get) match {
      case Some(gdalNoData) =>
        ArrayTile.fromBytes(bytes, metaData.cellType, cols, rows, gdalNoData)
      case None =>
        ArrayTile.fromBytes(bytes, metaData.cellType, cols, rows)
    }

  lazy val bandCount: Int =
    this &|->
      ImageDirectory._basicTags ^|->
      BasicTags._samplesPerPixel get

  lazy val bands: Seq[Tile] = {
    val tileBuffer = ListBuffer[Tile]()
    val tileSize = metaData.cellType.numBytes(cols * rows)

    cfor(0)(_ < bandCount, _ + 1) { i =>
      val arr = Array.ofDim[Byte](tileSize)
      System.arraycopy(imageBytes, i * tileSize, arr, 0, tileSize)

      tileBuffer += toTile(arr)
    }

    tileBuffer.toList
  }
}
