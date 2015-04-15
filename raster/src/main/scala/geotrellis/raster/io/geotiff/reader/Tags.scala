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
  val PkZipCoded = 32946

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

/**
  * The Orientations are named as such as the first position is where
  * the rows start and the second where the columns start.
  *
  * For example TopLeft means that the the 0th row is the top of the image
  * and the 0th column is the left of the image.
  */
object Orientations {

  val TopLeft = 1
  val TopRight = 2
  val BottomRight = 3
  val BottomLeft = 4
  val LeftTop = 5
  val RightTop = 6
  val RightBottom = 7
  val LeftBottom = 8

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
  val MetadataTag = 42112
  val GDALInternalNoDataTag = 42113

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

case class TagMetadata(
  tag: Int,
  fieldType: Int,
  length: Int,
  offset: Int
)

@Lenses("_")
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

@Lenses("_")
case class BasicTags(
  bitsPerSample: Option[Array[Int]] = None,
  colorMap: Seq[(Short, Short, Short)] = Seq(),
  imageLength: Int = 0,
  imageWidth: Int = 0,
  compression: Int = 1,
  photometricInterp: Int = -1,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Long = (1 << 31) - 1,
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Array[Int]] = None,
  stripOffsets: Option[Array[Int]] = None,
  xResolution: Option[(Long, Long)] = None,
  yResolution: Option[(Long, Long)] = None
)

@Lenses("_")
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
  orientation: Int = 1,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Int = 0,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Array[Int]] = None,
  predictor: Option[Int] = None
)

@Lenses("_")
case class GeoTiffTags(
  modelTiePoints: Option[Array[(Pixel3D, Pixel3D)]] = None,
  modelTransformation: Option[Array[Array[Double]]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Array[Double]] = None,
  asciis: Option[String] = None,
  metadata: Option[String] = None,
  gdalInternalNoData: Option[Double] = None
)

@Lenses("_")
case class DocumentationTags(
  documentName: Option[String] = None,
  pageName: Option[String] = None,
  pageNumber: Option[Array[Int]] = None,
  xPositions: Option[Array[(Long, Long)]] = None,
  yPositions: Option[Array[(Long, Long)]] = None
)

@Lenses("_")
case class TileTags(
  tileWidth: Option[Long] = None,
  tileLength: Option[Long] = None,
  tileOffsets: Option[Array[Int]] = None,
  tileByteCounts: Option[Array[Int]] = None
)

@Lenses("_")
case class CmykTags(
  inkSet: Option[Int] = None,
  numberOfInks: Option[Int] = None,
  inkNames: Option[String] = None,
  dotRange: Option[Array[Int]] = None,
  targetPrinters: Option[String] = None
)

@Lenses("_")
case class DataSampleFormatTags(
  sampleFormat: Array[Int] = Array(1),
  maxSampleValue: Option[Array[Long]] = None,
  minSampleValue: Option[Array[Long]] = None
)

@Lenses("_")
case class ColimetryTags(
  whitePoints: Option[Array[(Long, Long)]] = None,
  primaryChromaticities: Option[Array[(Long, Long)]] = None,
  transferFunction: Option[Array[Int]] = None,
  transferRange: Option[Array[Int]] = None,
  referenceBlackWhite: Option[Array[Long]] = None
)

@Lenses("_")
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

@Lenses("_")
case class YCbCrTags(
  yCbCrCoefficients: Option[Array[(Long, Long)]] = None,
  yCbCrSubSampling: Option[Array[Int]] = None,
  yCbCrPositioning: Option[Int] = None
)

@Lenses("_")
case class NonStandardizedTags(
  asciisMap: HashMap[Int, String] = HashMap[Int, String](),
  longsMap: HashMap[Int, Array[Long]] = HashMap[Int, Array[Long]](),
  fractionalsMap: HashMap[Int, Array[(Long, Long)]] = HashMap[Int, Array[(Long, Long)]](),
  undefinedMap: HashMap[Int, Array[Byte]] = HashMap[Int, Array[Byte]](),
  doublesMap: HashMap[Int, Array[Double]] = HashMap[Int, Array[Double]]()
)
