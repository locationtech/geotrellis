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

import scala.collection.immutable.HashMap

case class TagMetadata(tag: Int, fieldType: Int,
  length: Int, offset: Int)

case class ModelTiePoint(i: Double, j: Double, k: Double,
  x: Double, y: Double, z:Double)

case class MetadataTags(
  artist: Option[String] = None,
  copyright: Option[String] = None,
  dateTime: Option[String] = None,
  computer: Option[String] = None,
  imageDesc: Option[String] = None,
  maker: Option[String] = None,
  model: Option[String] = None,
  software: Option[String] = None
)

case class BasicTags(
  bitsPerSample: Vector[Int] = Vector.empty,
  colorMap: Option[Vector[Int]] = None,
  imageLength: Option[Long] = None,
  imageWidth: Option[Long] = None,
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
  fillOrder: Option[Int] = None,
  freeByteCounts: Option[Vector[Long]] = None,
  freeOffsets: Option[Vector[Long]] = None,
  grayResponseCurve: Option[Vector[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Long] = None,
  orientation: Option[Int] = None,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Option[Long] = None,
  t6Options: Option[Long] = None,
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
  pageNumbers: Option[Vector[Int]] = None,
  xPositions: Option[Vector[(Long, Long)]] = None,
  yPositions: Option[Vector[(Long, Long)]] = None
)

case class TileTags(
  tileWidth: Option[Long] = None,
  tileLength: Option[Long] = None,
  tileOffsets: Option[Vector[Long]] = None,
  tileByteCounts: Option[Vector[Long]] = None
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
  maxSampleValues: Option[Vector[Long]] = None,
  minSampleValues: Option[Vector[Long]] = None
)

case class ColimetryTags(
  whitePoints: Option[Vector[(Long, Long)]] = None,
  primaryChromaticities: Option[Vector[(Long, Long)]] = None,
  transferFunction: Option[Vector[Int]] = None,
  transferRange: Option[Vector[Int]] = None,
  referenceBlackWhite: Option[Vector[Long]] = None
)

case class JpegTags(
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
  imageBytes: Option[Vector[Byte]] = None
)

object ImageDirectoryLenses {

  val countLens = mkLens[ImageDirectory, Int]("count")

  val metaDataTagsLens = mkLens[ImageDirectory, MetadataTags]("metadataTags")

  val artistLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("artist")
  val copyrightLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("copyright")
  val dateTimeLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("dateTime")
  val computerLens = metaDataTagsLens |-> mkLens[MetadataTags,
    Option[String]]("computer")
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
    Vector[Int]]("bitsPerSample")
  val colorMapLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Vector[Int]]]("colorMap")
  val imageLengthLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Long]]("imageLength")
  val imageWidthLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Long]]("imageWidth")
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
  val fillOrderLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("fillOrder")
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
    Option[Long]]("t4Options")
  val t6OptionsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Long]]("t6Options")
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
  val pageNumbersLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[Int]]]("pageNumbers")
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
    Option[Vector[Long]]]("tileOffsets")
  val tileByteCountsLens = tileTagsLens |-> mkLens[TileTags,
    Option[Vector[Long]]]("tileByteCounts")

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
  val maxSampleValuesLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Long]]]("maxSampleValues")
  val minSampleValuesLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Long]]]("minSampleValues")

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

  val imageBytesLens = mkLens[ImageDirectory,
    Option[Vector[Byte]]]("imageBytes")

}
