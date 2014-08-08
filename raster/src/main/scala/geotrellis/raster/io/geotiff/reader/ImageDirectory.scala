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

import monocle.syntax._
import monocle.Macro._

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.io.arg.ArgWriter

import geotrellis.raster.io.geotiff.reader.utils.ArrayUtils._
import geotrellis.raster.io.geotiff.reader.utils.MatrixUtils._
import geotrellis.raster.io.geotiff.reader.CommonPublicValues._

import geotrellis.vector.Extent

import scala.collection.immutable.HashMap

object CompressionType {

  val Uncompressed = 1
  val HuffmanCoded = 2
  val GroupThreeCoded = 3
  val GroupFourCoded = 4
  val LZWCoded = 5
  val JpegOldCoded = 6
  val JpegCoded = 7
  val ZLibCoded = 8
  val PackBitsCoded = 32773

}

object TiffFieldType {

  val BytesFieldType = 1
  val AsciisFieldType = 2
  val ShortsFieldType = 3
  val IntsFieldType = 4
  val FractionalsFieldType = 5
  val SignedBytesFieldType = 6
  val UndefinedFieldType = 7
  val SignedShortsFieldType = 8
  val SignedIntsFieldType = 9
  val SignedFractionalsFieldType = 10
  val FloatsFieldType = 11
  val DoublesFieldType = 12

}

object SampleFormat {

  val UnsignedInt = 1
  val SignedInt = 2
  val FloatingPoint = 3
  val Undefined = 4

}

object Tags {

  val NewSubfileTypeTag = 254
  val SubfileTypeTag = 255
  val ImageWidthTag = 256
  val ImageLengthTag = 257
  val BitsPerSampleTag = 258
  val CompressionTag = 259
  val PhotometricInterpTag = 262
  val ThresholdingTag = 263
  val CellWidthTag = 264
  val CellLengthTag = 265
  val FillOrderTag = 266
  val DocumentNameTag = 269
  val ImageDescTag = 270
  val MakerTag = 271
  val ModelTag = 272
  val StripOffsetsTag = 273
  val OrientationTag = 274
  val SamplesPerPixelTag = 277
  val RowsPerStripTag = 278
  val StripByteCountsTag = 279
  val MinSampleValueTag = 280
  val MaxSampleValueTag = 281
  val XResolutionTag = 282
  val YResolutionTag = 283
  val PlanarConfigurationTag = 284
  val PageNameTag = 285
  val XPositionTag = 286
  val YPositionTag = 287
  val FreeOffsetsTag = 288
  val FreeByteCountsTag = 289
  val GrayResponseUnitTag = 290
  val GrayResponseCurveTag = 291
  val T4OptionsTag = 292
  val T6OptionsTag = 293
  val ResolutionUnitTag = 296
  val PageNumberTag = 297
  val TransferFunctionTag = 301
  val SoftwareTag = 305
  val DateTimeTag = 306
  val ArtistTag = 315
  val HostComputerTag = 316
  val PredictorTag = 317
  val WhitePointTag = 318
  val PrimaryChromaticitiesTag = 319
  val ColorMapTag = 320
  val HalftoneHintsTag = 321
  val TileWidthTag = 322
  val TileLengthTag = 323
  val TileOffsetsTag = 324
  val TileByteCountsTag = 325
  val InkSetTag = 332
  val InkNamesTag = 333
  val NumberOfInksTag = 334
  val DotRangeTag = 336
  val TargetPrinterTag = 337
  val ExtraSamplesTag = 338
  val SampleFormatTag = 339
  val TransferRangeTag = 342
  val JpegTablesTag = 347
  val JpegProcTag = 512
  val JpegInterchangeFormatTag = 513
  val JpegInterchangeFormatLengthTag = 514
  val JpegRestartIntervalTag = 515
  val JpegLosslessPredictorsTag = 517
  val JpegPointTransformsTag = 518
  val JpegQTablesTag = 519
  val JpegDCTablesTag = 520
  val JpegACTablesTag = 521
  val YCbCrCoefficientsTag = 529
  val YCbCrSubSamplingTag = 530
  val YCbCrPositioningTag = 531
  val ReferenceBlackWhiteTag = 532
  val CopyrightTag = 33432
  val ModelPixelScaleTag = 33550
  val ModelTiePointsTag = 33922
  val ModelTransformationTag = 34264
  val GeoKeyDirectoryTag = 34735
  val DoublesTag = 34736
  val AsciisTag = 34737

}

case class Coordinate(x: Double = 0, y: Double = 0)

case class GeoTiffCoordinates(
  minX: Coordinate = Coordinate(),
  minY: Coordinate = Coordinate(),
  maxX: Coordinate = Coordinate(),
  maxY: Coordinate = Coordinate()
)

object Pixel3D {

  def fromArray(v: Array[Double]): Pixel3D =
    if (v.size == 3) Pixel3D(v(0), v(1), v(2))
    else throw new IllegalArgumentException(
      "3D pixel needs vector with size 3 (x, y ,z)"
    )

}

case class Pixel3D(x: Double, y: Double, z: Double)

case class TagMetadata(tag: Int, fieldType: Int,
  length: Int, offset: Int)

case class MetadataTags(
  artist: Option[String] = None,
  copyright: Option[String] = None,
  dateTime: Option[String] = None,
  hostComputer: Option[String] = None,
  imageDesc: Option[String] = None,
  maker: Option[String] = None,
  model: Option[String] = None,
  software: Option[String] = None
)

case class BasicTags(
  bitsPerSample: Option[Array[Int]] = None,
  colorMap: Option[Array[Int]] = None,
  imageLength: Int = 0,
  imageWidth: Int = 0,
  compression: Int = 1,
  photometricInterp: Option[Int] = None,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Long = (1 << 31) - 1,
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Array[Int]] = None,
  stripOffsets: Option[Array[Int]] = None,
  xResolution: Option[(Long, Long)] = None,
  yResolution: Option[(Long, Long)] = None
)

case class NonBasicTags(
  cellLength: Option[Int] = None,
  cellWidth: Option[Int] = None,
  extraSamples: Option[Array[Int]] = None,
  fillOrder: Int = 1,
  freeByteCounts: Option[Array[Long]] = None,
  freeOffsets: Option[Array[Long]] = None,
  grayResponseCurve: Option[Array[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Long] = None,
  orientation: Option[Int] = None,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Int = 0,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Array[Int]] = None,
  predictor: Option[Int] = None
)

case class GeoTiffTags(
  modelTiePoints: Option[Array[(Pixel3D, Pixel3D)]] = None,
  modelTransformation: Option[Array[Array[Double]]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Array[Double]] = None,
  asciis: Option[String] = None
)

case class DocumentationTags(
  documentName: Option[String] = None,
  pageName: Option[String] = None,
  pageNumber: Option[Array[Int]] = None,
  xPositions: Option[Array[(Long, Long)]] = None,
  yPositions: Option[Array[(Long, Long)]] = None
)

case class TileTags(
  tileWidth: Option[Long] = None,
  tileLength: Option[Long] = None,
  tileOffsets: Option[Array[Int]] = None,
  tileByteCounts: Option[Array[Int]] = None
)

case class CmykTags(
  inkSet: Option[Int] = None,
  numberOfInks: Option[Int] = None,
  inkNames: Option[String] = None,
  dotRange: Option[Array[Int]] = None,
  targetPrinters: Option[String] = None
)

case class DataSampleFormatTags(
  sampleFormat: Array[Int] = Array(1),
  maxSampleValue: Option[Array[Long]] = None,
  minSampleValue: Option[Array[Long]] = None
)

case class ColimetryTags(
  whitePoints: Option[Array[(Long, Long)]] = None,
  primaryChromaticities: Option[Array[(Long, Long)]] = None,
  transferFunction: Option[Array[Int]] = None,
  transferRange: Option[Array[Int]] = None,
  referenceBlackWhite: Option[Array[Long]] = None
)

case class JpegTags(
  jpegTables: Option[Array[Byte]] = None,
  jpegProc: Option[Int] = None,
  jpegInterchangeFormat: Option[Long] = None,
  jpegInterchangeFormatLength: Option[Long] = None,
  jpegRestartInterval: Option[Int] = None,
  jpegLosslessPredictors: Option[Array[Int]] = None,
  jpegPointTransforms: Option[Array[Int]] = None,
  jpegQTables: Option[Array[Long]] = None,
  jpegDCTables: Option[Array[Long]] = None,
  jpegACTables: Option[Array[Long]] = None
)

case class YCbCrTags(
  yCbCrCoefficients: Option[Array[(Long, Long)]] = None,
  yCbCrSubSampling: Option[Array[Int]] = None,
  yCbCrPositioning: Option[Int] = None
)

case class NonStandardizedTags(
  asciisMap: HashMap[Int, String] = HashMap[Int, String](),
  longsMap: HashMap[Int, Array[Long]] = HashMap[Int, Array[Long]](),
  fractionalsMap: HashMap[Int, Array[(Long, Long)]] = HashMap[Int,
    Array[(Long, Long)]](),
  undefinedMap: HashMap[Int, Array[Byte]] = HashMap[Int, Array[Byte]](),
  doublesMap: HashMap[Int, Array[Double]] = HashMap[Int, Array[Double]]()
)

case class ImageDirectory(
  count: Int,
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

  import ImageDirectoryLenses._
  import GeoKeyDirectoryLenses._

  def hasStripStorage(): Boolean = (this |-> tileWidthLens get).isEmpty

  def tileBitsSize(): Option[Long] =
    ((this |-> tileWidthLens get), (this |-> tileLengthLens get)) match {
      case (Some(tileWidth), Some(tileLength)) =>
        Some(bitsPerPixel * tileWidth * tileLength)
      case _ => None
    }

  def rowsInStrip(index: Int): Option[Long] = if (hasStripStorage) {
    this |-> stripByteCountsLens get match {
      case Some(stripByteCounts) => {
        val rowsPerStrip = this |-> rowsPerStripLens get
        val imageLength = this |-> imageLengthLens get
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

  def rowsInSegment(index: Int): Int = if (hasStripStorage)
    rowsInStrip(index).get.toInt
  else
    (this |-> tileLengthLens get).get.toInt

  def bitsPerPixel(): Int = this |-> bitsPerSampleLens get match {
    case Some(v) => v.sum
    case None => this |-> samplesPerPixelLens get
  }

  // TODO: Isn't this just row size? Rows need to all be the same length.
  def imageSegmentByteSize(index: Option[Int] = None): Long =
    (imageSegmentBitsSize(index) + 7) / 8

  def imageSegmentBitsSize(index: Option[Int] = None): Long =
    if (hasStripStorage && !index.isEmpty)
      rowsInStrip(index.get).get * (this |-> imageWidthLens get) * bitsPerPixel
    else tileBitsSize.get * bitsPerPixel

  def rowSize(): Int = (if (hasStripStorage) (this |-> imageWidthLens get)
  else (this |-> tileWidthLens get).get).toInt

  def getRasterBoundaries: Array[Pixel3D] = {
    val imageWidth = (this |-> imageWidthLens get).toInt
    val imageLength = (this |-> imageLengthLens get).toInt

    Array(
      Pixel3D(0, imageLength, getDoubleValue(0, imageLength - 1)),
      Pixel3D(imageWidth, 0, getDoubleValue(imageWidth - 1, 0))
    )
  }

  def getDoubleValue(x: Int, y: Int): Double = {
    val imageWidth = (this |-> imageWidthLens get).toInt
    val imageLength = (this |-> imageLengthLens get).toInt

    val index = y * imageLength + x

    if (x >= imageWidth || y >= imageLength) throw new IllegalArgumentException(
      s"x or y out of bounds x: $x, y: $y, imageWidth: $imageWidth, imageLength: $imageLength"
    ) else cellType match {
      case TypeBit => {
        val byteIndex = index / 8
        val bitIndex = index % 8

        ((imageBytes(byteIndex) & (1 << bitIndex)) >> bitIndex).toDouble
      }
      case TypeByte => imageBytes.readIntNumber(1, index).toDouble
      case TypeShort => imageBytes.readIntNumber(2, index).toDouble
      case TypeInt => imageBytes.readIntNumber(4, index).toDouble
      case TypeFloat => imageBytes.readFloatPointNumber(4, index)
      case TypeDouble => imageBytes.readFloatPointNumber(8, index)
    }
  }

  // What about multiple samples?
  def cellType(): CellType =
    (this |-> bitsPerSampleLens get, this |-> sampleFormatLens get) match {
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
              "bad/unsupported bitspersample or sampleformat"
            )
          }

      case _ => throw new MalformedGeoTiffException("no bitsPerSample values!")
    }

  def toRaster(): (ArrayTile, Extent) = {
    val cols = this |-> imageWidthLens get
    val rows = this |-> imageLengthLens get

    val tile = ArrayTile.fromBytes(imageBytes.toArray, cellType, cols, rows)

    (tile, extent)
  }

  def writeRasterToArg(path: String, imageName: String): Unit = {
    writeRasterToArg(path, imageName, cellType, toRaster._1)
  }

  def writeRasterToArg(path: String, imageName: String, cellType: CellType,
    raster: ArrayTile): Unit =
    new ArgWriter(cellType).write(path, raster, extent, imageName)

  lazy val geoKeyDirectory = geoTiffTags.geoKeyDirectory.getOrElse {
    throw new IllegalAccessException("no geo key directory present")
  }

  lazy val extent: Extent = (this |-> modelTransformationLens get) match {
    case Some(trans) if (trans.validateAsMatrix && trans.size == 4
        && trans(0).size == 4) => transformationModelSpace(trans)
    case _ => (this |-> modelTiePointsLens get) match {
      case Some(tiePoints) if (!tiePoints.isEmpty) =>
        tiePointsModelSpace(tiePoints, this |-> modelPixelScaleLens get)
      case _ => throw new MalformedGeoTiffException(
        "neither model transformation nor tiepoints, or malformed ones."
      )
    }
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

          Pixel3D(scaleX + second.x, scaleY + second.y, scaleZ + second.z)
        }

        getExtentFromModelFunction(modelFunc)
      }
      case None => {
        val imageWidth = (this |-> imageWidthLens get).toInt
        val imageLength = (this |-> imageLengthLens get).toInt

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

  // if pixel is point then problemo
  private def getExtentFromModelFunction(func: Pixel3D => Pixel3D) = {
    val modelPixels = getRasterBoundaries.map(func)

    val (minX, minY) = (modelPixels(0).x, modelPixels(1).y)
    val (maxX, maxY) = (modelPixels(1).x, modelPixels(0).y)

    Extent(minX, minY, maxX, maxY)
  }

  private def hasPixelArea(): Boolean =
    (geoKeyDirectory |-> gtRasterTypeLens get) match {
      case Some(UndefinedCPV) => throw new MalformedGeoTiffException(
        "the raster type must be present."
      )
      case Some(UserDefinedCPV) => throw new GeoTiffReaderLimitationException(
        "this reader doesn't support user defined raster types."
      )
      case Some(v) => v == 1
      case None => true
    }

}

object ImageDirectoryLenses {

  val countLens = mkLens[ImageDirectory, Int]("count")

  val metaDataTagsLens = mkLens[ImageDirectory, MetadataTags]("metadataTags")

  val artistLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("artist")
  val copyrightLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("copyright")
  val dateTimeLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("dateTime")
  val hostComputerLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("hostComputer")
  val imageDescLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("imageDesc")
  val makerLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("maker")
  val modelLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("model")
  val softwareLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("software")

  val basicTagsLens = mkLens[ImageDirectory, BasicTags]("basicTags")

  val bitsPerSampleLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Array[Int]]]("bitsPerSample")
  val colorMapLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Array[Int]]]("colorMap")
  val imageLengthLens = basicTagsLens |-> mkLens[BasicTags, Int]("imageLength")
  val imageWidthLens = basicTagsLens |-> mkLens[BasicTags, Int]("imageWidth")
  val compressionLens = basicTagsLens |-> mkLens[BasicTags,
    Int]("compression")
  val photometricInterpLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Int]]("photometricInterp")
  val resolutionUnitLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Int]]("resolutionUnit")
  val rowsPerStripLens = basicTagsLens |-> mkLens[BasicTags,
    Long]("rowsPerStrip")
  val samplesPerPixelLens = basicTagsLens |-> mkLens[BasicTags,
    Int]("samplesPerPixel")
  val stripByteCountsLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Array[Int]]]("stripByteCounts")
  val stripOffsetsLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Array[Int]]]("stripOffsets")
  val xResolutionLens = basicTagsLens |-> mkLens[BasicTags,
    Option[(Long, Long)]]("xResolution")
  val yResolutionLens = basicTagsLens |-> mkLens[BasicTags,
    Option[(Long, Long)]]("yResolution")

  val nonBasicTagsLens = mkLens[ImageDirectory, NonBasicTags]("nonBasicTags")

  val cellLengthLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("cellLength")
  val cellWidthLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("cellWidth")
  val extraSamplesLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Array[Int]]]("extraSamples")
  val fillOrderLens = nonBasicTagsLens |-> mkLens[NonBasicTags, Int]("fillOrder")
  val freeByteCountsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Array[Long]]]("freeByteCounts")
  val freeOffsetsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Array[Long]]]("freeOffsets")
  val grayResponseCurveLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Array[Int]]]("grayResponseCurve")
  val grayResponseUnitLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("grayResponseUnit")
  val newSubfileTypeLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Long]]("newSubfileType")
  val orientationLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("orientation")
  val planarConfigurationLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("planarConfiguration")
  val subfileTypeLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("subfileType")
  val thresholdingLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Int]("thresholding")
  val t4OptionsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Int]("t4Options")
  val t6OptionsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("t6Options")
  val halftoneHintsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Array[Int]]]("halftoneHints")
  val predictorLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("predictor")

  val geoTiffTagsLens = mkLens[ImageDirectory, GeoTiffTags]("geoTiffTags")

  val modelTiePointsLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Array[(Pixel3D, Pixel3D)]]]("modelTiePoints")
  val modelPixelScaleLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[(Double, Double, Double)]]("modelPixelScale")
  val modelTransformationLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Array[Array[Double]]]]("modelTransformation")
  val geoKeyDirectoryLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[GeoKeyDirectory]]("geoKeyDirectory")
  val doublesLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Array[Double]]]("doubles")
  val asciisLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[String]]("asciis")

  val documentationTagsLens = mkLens[ImageDirectory,
    DocumentationTags]("documentationTags")

  val documentNameLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[String]]("documentName")
  val pageNameLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[String]]("pageName")
  val pageNumberLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Array[Int]]]("pageNumber")
  val xPositionsLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Array[(Long, Long)]]]("xPositions")
  val yPositionsLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Array[(Long, Long)]]]("yPositions")

  val tileTagsLens = mkLens[ImageDirectory, TileTags]("tileTags")

  val tileWidthLens = tileTagsLens |-> mkLens[TileTags,
    Option[Long]]("tileWidth")
  val tileLengthLens = tileTagsLens |-> mkLens[TileTags,
    Option[Long]]("tileLength")
  val tileOffsetsLens = tileTagsLens |-> mkLens[TileTags,
    Option[Array[Int]]]("tileOffsets")
  val tileByteCountsLens = tileTagsLens |-> mkLens[TileTags,
    Option[Array[Int]]]("tileByteCounts")

  val cmykTagsLens = mkLens[ImageDirectory, CmykTags]("cmykTags")

  val inkSetLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[Int]]("inkSet")
  val numberOfInksLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[Int]]("numberOfInks")
  val inkNamesLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[String]]("inkNames")
  val dotRangeLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[Array[Int]]]("dotRange")
  val targetPrintersLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[String]]("targetPrinters")

  val dataSampleFormatTagsLens = mkLens[ImageDirectory,
    DataSampleFormatTags]("dataSampleFormatTags")

  val sampleFormatLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Array[Int]]("sampleFormat")
  val maxSampleValueLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Array[Long]]]("maxSampleValue")
  val minSampleValueLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Array[Long]]]("minSampleValue")

  val colimetryTagsLens = mkLens[ImageDirectory, ColimetryTags]("colimetryTags")

  val whitePointsLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Array[(Long, Long)]]]("whitePoints")
  val primaryChromaticitiesLens = colimetryTagsLens |-> mkLens[
    ColimetryTags, Option[Array[(Long, Long)]]]("primaryChromaticities")
  val transferFunctionLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Array[Int]]]("transferFunction")
  val transferRangeLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Array[Int]]]("transferRange")
  val referenceBlackWhiteLens = colimetryTagsLens |-> mkLens[
    ColimetryTags,  Option[Array[Long]]]("referenceBlackWhite")

  val jpegTagsLens = mkLens[ImageDirectory, JpegTags]("jpegTags")

  val jpegTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Array[Byte]]]("jpegTables")
  val jpegProcLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegProc")
  val jpegInterchangeFormatLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Long]]("jpegInterchangeFormat")
  val jpegInterchangeFormatLengthLens = jpegTagsLens |-> mkLens[
    JpegTags, Option[Long]]("jpegInterchangeFormatLength")
  val jpegRestartIntervalLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegRestartInterval")
  val jpegLosslessPredictorsLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Array[Int]]]("jpegLosslessPredictors")
  val jpegPointTransformsLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Array[Int]]]("jpegPointTransforms")
  val jpegQTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Array[Long]]]("jpegQTables")
  val jpegDCTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Array[Long]]]("jpegDCTables")
  val jpegACTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Array[Long]]]("jpegACTables")

  val yCbCrTagsLens = mkLens[ImageDirectory, YCbCrTags]("yCbCrTags")

  val yCbCrCoefficientsLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Array[(Long, Long)]]]("yCbCrCoefficients")
  val yCbCrSubSamplingLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Array[Int]]]("yCbCrSubSampling")
  val yCbCrPositioningLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Int]]("yCbCrPositioning")

  val nonStandardizedTagsLens = mkLens[ImageDirectory,
    NonStandardizedTags]("nonStandardizedTags")

  val asciisMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, String]]("asciisMap")
  val longsMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, Array[Long]]]("longsMap")
  val fractionalsMapLens = nonStandardizedTagsLens |-> mkLens[
    NonStandardizedTags, HashMap[Int, Array[(Long, Long)]]]("fractionalsMap")
  val undefinedMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, Array[Byte]]]("undefinedMap")
  val doublesMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, Array[Double]]]("doublesMap")

  val imageBytesLens = mkLens[ImageDirectory, Array[Byte]]("imageBytes")
}
