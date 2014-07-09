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

package geotrellis.io.geotiff

import monocle.syntax._
import monocle.Macro._

import geotrellis._
import geotrellis.raster._

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

case class TagMetadata(tag: Int, fieldType: Int,
  length: Int, offset: Int)

case class ModelTiePoint(i: Double, j: Double, k: Double,
  x: Double, y: Double, z:Double)

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
  bitsPerSample: Option[Vector[Int]] = None,
  colorMap: Option[Vector[Int]] = None,
  imageLength: Long = 0,
  imageWidth: Long = 0,
  compression: Int = 1,
  photometricInterp: Option[Int] = None,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Long = (1 << 31) - 1,
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Vector[Int]] = None,
  stripOffsets: Option[Vector[Int]] = None,
  xResolution: Option[(Long, Long)] = None,
  yResolution: Option[(Long, Long)] = None
)

case class NonBasicTags(
  cellLength: Option[Int] = None,
  cellWidth: Option[Int] = None,
  extraSamples: Option[Vector[Int]] = None,
  fillOrder: Int = 1,
  freeByteCounts: Option[Vector[Long]] = None,
  freeOffsets: Option[Vector[Long]] = None,
  grayResponseCurve: Option[Vector[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Long] = None,
  orientation: Option[Int] = None,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Int = 0,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Vector[Int]] = None,
  predictor: Option[Int] = None
)

case class GeoTiffTags(
  modelTiePoints: Option[Vector[ModelTiePoint]] = None,
  modelTransformation: Option[Vector[Double]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Vector[Double]] = None,
  asciis: Option[String] = None
)

case class DocumentationTags(
  documentName: Option[String] = None,
  pageName: Option[String] = None,
  pageNumber: Option[Vector[Int]] = None,
  xPositions: Option[Vector[(Long, Long)]] = None,
  yPositions: Option[Vector[(Long, Long)]] = None
)

case class TileTags(
  tileWidth: Option[Long] = None,
  tileLength: Option[Long] = None,
  tileOffsets: Option[Vector[Int]] = None,
  tileByteCounts: Option[Vector[Int]] = None
)

case class CmykTags(
  inkSet: Option[Int] = None,
  numberOfInks: Option[Int] = None,
  inkNames: Option[String] = None,
  dotRange: Option[Vector[Int]] = None,
  targetPrinters: Option[String] = None
)

case class DataSampleFormatTags(
  sampleFormat: Option[Vector[Int]] = None,
  maxSampleValue: Option[Vector[Long]] = None,
  minSampleValue: Option[Vector[Long]] = None
)

case class ColimetryTags(
  whitePoints: Option[Vector[(Long, Long)]] = None,
  primaryChromaticities: Option[Vector[(Long, Long)]] = None,
  transferFunction: Option[Vector[Int]] = None,
  transferRange: Option[Vector[Int]] = None,
  referenceBlackWhite: Option[Vector[Long]] = None
)

case class JpegTags(
  jpegTables: Option[Vector[Byte]] = None,
  jpegProc: Option[Int] = None,
  jpegInterchangeFormat: Option[Long] = None,
  jpegInterchangeFormatLength: Option[Long] = None,
  jpegRestartInterval: Option[Int] = None,
  jpegLosslessPredictors: Option[Vector[Int]] = None,
  jpegPointTransforms: Option[Vector[Int]] = None,
  jpegQTables: Option[Vector[Long]] = None,
  jpegDCTables: Option[Vector[Long]] = None,
  jpegACTables: Option[Vector[Long]] = None
)

case class YCbCrTags(
  yCbCrCoefficients: Option[Vector[(Long, Long)]] = None,
  yCbCrSubSampling: Option[Vector[Int]] = None,
  yCbCrPositioning: Option[Int] = None
)

case class NonStandardizedTags(
  asciisMap: HashMap[Int, String] = HashMap[Int, String](),
  longsMap: HashMap[Int, Vector[Long]] = HashMap[Int, Vector[Long]](),
  fractionalsMap: HashMap[Int, Vector[(Long, Long)]] = HashMap[Int,
    Vector[(Long, Long)]](),
  undefinedMap: HashMap[Int, Vector[Byte]] = HashMap[Int, Vector[Byte]](),
  doublesMap: HashMap[Int, Vector[Double]] = HashMap[Int, Vector[Double]]()
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
  imageBytes: Vector[Byte] = Vector[Byte]()
) {

  import ImageDirectoryLenses._

  def hasStripStorage(): Boolean = (this |-> tileOffsetsLens get).isEmpty

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

  def imageSegmentBitsSize(index: Option[Int] = None): Long =
    if (hasStripStorage && !index.isEmpty)
      rowsInStrip(index.get).get * (this |-> imageWidthLens get) * bitsPerPixel
    else tileBitsSize.get

  def rowSize(): Int = (if (hasStripStorage) (this |-> imageWidthLens get)
  else (this |-> tileWidthLens get).get).toInt

  def toRaster(): Raster = {
    val cols = this |-> imageWidthLens get
    val rows = this |-> imageLengthLens get

    // How do we get the xmin, ymin, xmax, ymax coordinates
    // of the geographical envelope of the GeoTIFF?
    val extent: Extent = Extent(293518.1886150768,5680494.194041155,890338.5054657329,6267530.571271311)

    val bytes: Array[Byte] = imageBytes.toArray

    this |-> bitsPerSampleLens get match {
      case Some(bitsPerSampleArray) if (bitsPerSampleArray.size > 0) => {
        val bitsPerSample = bitsPerSampleArray(0)
        val sampleFormat = this |-> sampleFormatLens get

        import SampleFormat._

        val cellType =
          if (bitsPerSample == 1)
            TypeBit
          else if (bitsPerSample <= 8)
            TypeByte
          else if (bitsPerSample <= 16)
            TypeShort
          else if (bitsPerSample == 32 && sampleFormat == UnsignedInt
            || sampleFormat == SignedInt)
            TypeInt
          else if (bitsPerSample == 32 && sampleFormat == FloatingPoint)
            TypeFloat
          else if (bitsPerSample == 64)
            TypeDouble
          else throw new MalformedGeoTiffException("bad bitspersample or sampleformat")

        // if( poDS->nBitsPerSample <= 8 )
        // {
        //     eDataType = GDT_Byte;
        //     if( nSampleFormat == SAMPLEFORMAT_INT )
        //         SetMetadataItem( "PIXELTYPE", "SIGNEDBYTE", "IMAGE_STRUCTURE" );

        // }
        // else if( poDS->nBitsPerSample <= 16 )
        // {
        //     if( nSampleFormat == SAMPLEFORMAT_INT )
        //         eDataType = GDT_Int16;
        //     else
        //         eDataType = GDT_UInt16;
        // }
        // else if( poDS->nBitsPerSample == 32 )
        // {
        //     if( nSampleFormat == SAMPLEFORMAT_COMPLEXINT )
        //         eDataType = GDT_CInt16;
        //     else if( nSampleFormat == SAMPLEFORMAT_IEEEFP )
        //         eDataType = GDT_Float32;
        //     else if( nSampleFormat == SAMPLEFORMAT_INT )
        //         eDataType = GDT_Int32;
        //     else
        //         eDataType = GDT_UInt32;
        // }
        // else if( poDS->nBitsPerSample == 64 )
        // {
        //     if( nSampleFormat == SAMPLEFORMAT_IEEEFP )
        //         eDataType = GDT_Float64;
        //     else if( nSampleFormat == SAMPLEFORMAT_COMPLEXIEEEFP )
        //         eDataType = GDT_CFloat32;
        //     else if( nSampleFormat == SAMPLEFORMAT_COMPLEXINT )
        //         eDataType = GDT_CInt32;
        // }
        // else if( poDS->nBitsPerSample == 128 )
        // {
        //     if( nSampleFormat == SAMPLEFORMAT_COMPLEXIEEEFP )
        //         eDataType = GDT_CFloat64;
        // }


        val rd = RasterData.fromArrayByte(bytes, cellType, cols.toInt, rows.toInt)

        val r = Raster(rd, RasterExtent(extent, cols, rows))

        // Write to core-test/data/data
        import geotrellis.data.arg.ArgWriter
        val path = "/Users/johanstenberg/Documents/programmering/GSOC/eclipse-geotrellis/geotrellis/core-test/data/data/sampleimage1.arg"
        new ArgWriter(cellType).write(path, r, "sampleimage1")

        r
      }
      case _ => throw new MalformedGeoTiffException("no bits per sample!")
    }
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
    Option[Vector[Int]]]("bitsPerSample")
  val colorMapLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Vector[Int]]]("colorMap")
  val imageLengthLens = basicTagsLens |-> mkLens[BasicTags, Long]("imageLength")
  val imageWidthLens = basicTagsLens |-> mkLens[BasicTags, Long]("imageWidth")
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
    Option[Vector[Int]]]("stripByteCounts")
  val stripOffsetsLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Vector[Int]]]("stripOffsets")
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
    Option[Vector[Int]]]("extraSamples")
  val fillOrderLens = nonBasicTagsLens |-> mkLens[NonBasicTags, Int]("fillOrder")
  val freeByteCountsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Vector[Long]]]("freeByteCounts")
  val freeOffsetsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Vector[Long]]]("freeOffsets")
  val grayResponseCurveLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Vector[Int]]]("grayResponseCurve")
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
    Option[Vector[Int]]]("halftoneHints")
  val predictorLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("predictor")

  val geoTiffTagsLens = mkLens[ImageDirectory, GeoTiffTags]("geoTiffTags")

  val modelTiePointsLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Vector[ModelTiePoint]]]("modelTiePoints")
  val modelPixelScaleLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[(Double, Double, Double)]]("modelPixelScale")
  val modelTransformationLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Vector[Double]]]("modelTransformation")
  val geoKeyDirectoryLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[GeoKeyDirectory]]("geoKeyDirectory")
  val doublesLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Vector[Double]]]("doubles")
  val asciisLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[String]]("asciis")

  val documentationTagsLens = mkLens[ImageDirectory,
    DocumentationTags]("documentationTags")

  val documentNameLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[String]]("documentName")
  val pageNameLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[String]]("pageName")
  val pageNumberLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[Int]]]("pageNumber")
  val xPositionsLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[(Long, Long)]]]("xPositions")
  val yPositionsLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[(Long, Long)]]]("yPositions")

  val tileTagsLens = mkLens[ImageDirectory, TileTags]("tileTags")

  val tileWidthLens = tileTagsLens |-> mkLens[TileTags,
    Option[Long]]("tileWidth")
  val tileLengthLens = tileTagsLens |-> mkLens[TileTags,
    Option[Long]]("tileLength")
  val tileOffsetsLens = tileTagsLens |-> mkLens[TileTags,
    Option[Vector[Int]]]("tileOffsets")
  val tileByteCountsLens = tileTagsLens |-> mkLens[TileTags,
    Option[Vector[Int]]]("tileByteCounts")

  val cmykTagsLens = mkLens[ImageDirectory, CmykTags]("cmykTags")

  val inkSetLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[Int]]("inkSet")
  val numberOfInksLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[Int]]("numberOfInks")
  val inkNamesLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[String]]("inkNames")
  val dotRangeLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[Vector[Int]]]("dotRange")
  val targetPrintersLens = cmykTagsLens |-> mkLens[CmykTags,
    Option[String]]("targetPrinters")

  val dataSampleFormatTagsLens = mkLens[ImageDirectory,
    DataSampleFormatTags]("dataSampleFormatTags")

  val sampleFormatLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Int]]]("sampleFormat")
  val maxSampleValueLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Long]]]("maxSampleValue")
  val minSampleValueLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Long]]]("minSampleValue")

  val colimetryTagsLens = mkLens[ImageDirectory, ColimetryTags]("colimetryTags")

  val whitePointsLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Vector[(Long, Long)]]]("whitePoints")
  val primaryChromaticitiesLens = colimetryTagsLens |-> mkLens[
    ColimetryTags, Option[Vector[(Long, Long)]]]("primaryChromaticities")
  val transferFunctionLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Vector[Int]]]("transferFunction")
  val transferRangeLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Vector[Int]]]("transferRange")
  val referenceBlackWhiteLens = colimetryTagsLens |-> mkLens[
    ColimetryTags,  Option[Vector[Long]]]("referenceBlackWhite")

  val jpegTagsLens = mkLens[ImageDirectory, JpegTags]("jpegTags")

  val jpegTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Byte]]]("jpegTables")
  val jpegProcLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegProc")
  val jpegInterchangeFormatLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Long]]("jpegInterchangeFormat")
  val jpegInterchangeFormatLengthLens = jpegTagsLens |-> mkLens[
    JpegTags, Option[Long]]("jpegInterchangeFormatLength")
  val jpegRestartIntervalLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegRestartInterval")
  val jpegLosslessPredictorsLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegLosslessPredictors")
  val jpegPointTransformsLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegPointTransforms")
  val jpegQTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Long]]]("jpegQTables")
  val jpegDCTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Long]]]("jpegDCTables")
  val jpegACTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Long]]]("jpegACTables")

  val yCbCrTagsLens = mkLens[ImageDirectory, YCbCrTags]("yCbCrTags")

  val yCbCrCoefficientsLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Vector[(Long, Long)]]]("yCbCrCoefficients")
  val yCbCrSubSamplingLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Vector[Int]]]("yCbCrSubSampling")
  val yCbCrPositioningLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Int]]("yCbCrPositioning")

  val nonStandardizedTagsLens = mkLens[ImageDirectory,
    NonStandardizedTags]("nonStandardizedTags")

  val asciisMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, String]]("asciisMap")
  val longsMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, Vector[Long]]]("longsMap")
  val fractionalsMapLens = nonStandardizedTagsLens |-> mkLens[
    NonStandardizedTags, HashMap[Int, Vector[(Long, Long)]]]("fractionalsMap")
  val undefinedMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, Vector[Byte]]]("undefinedMap")
  val doublesMapLens = nonStandardizedTagsLens |-> mkLens[NonStandardizedTags,
    HashMap[Int, Vector[Double]]]("doublesMap")

  val imageBytesLens = mkLens[ImageDirectory, Vector[Byte]]("imageBytes")

}
