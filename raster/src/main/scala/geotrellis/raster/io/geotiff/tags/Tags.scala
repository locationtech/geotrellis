package geotrellis.raster.io.geotiff.tags

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.utils._
import CommonPublicValues._

import geotrellis.vector.Extent

import geotrellis.proj4.CRS
import geotrellis.proj4.LatLng

import collection.immutable.{HashMap, Map}

import collection.mutable.ListBuffer

import xml._

import monocle.syntax._
import monocle.macros.Lenses

import spire.syntax.cfor._
import scala.collection.mutable

@Lenses("_")
case class Tags(
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
  nonStandardizedTags: NonStandardizedTags = NonStandardizedTags()
) {

  def compression = (this &|->
    Tags._basicTags ^|->
    BasicTags._compression get)

  def hasStripStorage(): Boolean = (this &|->
    Tags._tileTags ^|->
    TileTags._tileWidth get).isEmpty

  def rowsInStrip(index: Int): Option[Long] = 
    if (hasStripStorage) {
      (this &|->
        Tags._basicTags ^|->
        BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) => {
          val rowsPerStrip = (this &|->
            Tags._basicTags ^|->
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
        Tags._tileTags ^|->
        TileTags._tileLength get).get.toInt

  def bitsPerPixel(): Int = (this &|->
    Tags._basicTags ^|->
    BasicTags._bitsPerSample get) match {
    case Some(v) => v.sum
    case None => (this &|->
        Tags._basicTags ^|->
        BasicTags._samplesPerPixel get)
  }

  def bytesPerPixel: Int =
    (this.bitsPerPixel + 7) / 8

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
          Tags._tileTags ^|->
          TileTags._tileWidth get),
        (this &|->
          Tags._tileTags ^|->
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
    else (this &|-> Tags._tileTags ^|-> TileTags._tileWidth get).get.toInt

  def cols = (this &|-> Tags._basicTags ^|-> BasicTags._imageWidth get)
  def rows = (this &|-> Tags._basicTags ^|-> BasicTags._imageLength get)

  def extent: Extent = 
    (this 
      &|-> Tags._geoTiffTags
      ^|-> GeoTiffTags._modelTransformation get
    ) match {
      case Some(trans) if (trans.validateAsMatrix && trans.size == 4 && trans(0).size == 4) => 
        transformationModelSpace(trans)
      case _ => 
        (this 
          &|-> Tags._geoTiffTags
          ^|-> GeoTiffTags._modelTiePoints get
        ) match {
          case Some(tiePoints) if (!tiePoints.isEmpty) =>
            tiePointsModelSpace(
              tiePoints,
              (this 
                &|-> Tags._geoTiffTags
                ^|-> GeoTiffTags._modelPixelScale get
              )
            )
          case _ =>
            Extent(0, 0, cols, rows)
        }
    }

  def bandType: BandType =
    ((this &|-> Tags._basicTags
      ^|-> BasicTags._bitsPerSample get),
      (this &|-> Tags._dataSampleFormatTags
        ^|-> DataSampleFormatTags._sampleFormat get)) match {
      case (Some(bitsPerSampleArray), sampleFormatArray)
          if (bitsPerSampleArray.size > 0 && sampleFormatArray.size > 0) => {
            val bitsPerSample = bitsPerSampleArray(0)
            val sampleFormat = sampleFormatArray(0)

            BandType(bitsPerSample, sampleFormat)
          }

      case _ =>
        throw new MalformedGeoTiffException("no bitsPerSample values!")
    }

  def crs: CRS = proj4String match {
    case Some(s) => CRS.fromString(s)
    case None => LatLng
  }

  def geoKeyDirectory = geoTiffTags.geoKeyDirectory.getOrElse {
    throw new IllegalAccessException("no geo key directory present")
  }

  private def getRasterBoundaries: Array[Pixel3D] = {
    val imageWidth = cols
    val imageLength = rows

    Array(
      Pixel3D(0, imageLength, 0),
      Pixel3D(imageWidth, 0, 0)
    )
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

  def setGDALNoData(input: String) = (this &|-> Tags._geoTiffTags
    ^|-> GeoTiffTags._gdalInternalNoData set (parseGDALNoDataString(input)))

  def proj4String: Option[String] = try {
    GeoTiffCSParser(this).getProj4String
  } catch {
    case e: Exception => None
  }

  lazy val (tags, bandTags): (Map[String, String], Map[Int, Map[String, String]]) =
    (
      (this &|->
        Tags._basicTags ^|->
        BasicTags._samplesPerPixel get),
      (this &|->
        Tags._geoTiffTags ^|->
        GeoTiffTags._metadata get)
    ) match {
      case (numberOfBands, Some(str)) => {
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

        val bandsMetadataBuffer = mutable.Map[Int, Map[String, String]]()

        cfor(1)(_ <= numberOfBands, _ + 1) { i =>
          bandsMetadataMap.get(i - 1) match {
            case Some(map) => bandsMetadataBuffer(i) = map
            case None => bandsMetadataBuffer(i) = Map()
          }
        }

        (metadata, bandsMetadataBuffer.toMap)
      }
      case (numberOfBands, None) => (
        Map(),
        (1 to numberOfBands).map((_, Map[String, String]())).toMap
      )
    }

  private def metadataNodeSeqToMap(ns: NodeSeq): Map[String, String] =
    ns.map(s => ((s \ "@name").text -> s.text)).toMap

  def bandCount: Int =
    this &|->
      Tags._basicTags ^|->
      BasicTags._samplesPerPixel get

  def segmentCount: Int =
    if (hasStripStorage) {
      (this 
        &|-> Tags._basicTags 
        ^|-> BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) =>
          stripByteCounts.size
        case None => 
          throw new MalformedGeoTiffException("No StripByteCount information.")
      }
    } else {
      (this 
        &|-> Tags._tileTags 
        ^|-> TileTags._tileOffsets get) match {
        case Some(tileOffsets) =>
          tileOffsets.size
        case None =>
          throw new MalformedGeoTiffException("No TileOffsets information.")
      }
    }
}
