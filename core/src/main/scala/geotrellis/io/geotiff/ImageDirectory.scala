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
  imageLength: Option[Int] = None,
  imageWidth: Option[Int] = None,
  compression: Int = 1,
  photometricInterp: Option[Int] = None,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Int = (1 << 31) - 1,
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Vector[Int]] = None,
  stripOffsets: Option[Vector[Int]] = None,
  xResolution: Option[(Int, Int)] = None,
  yResolution: Option[(Int, Int)] = None
)

case class NonBasicTags(
  cellLength: Option[Int] = None,
  cellWidth: Option[Int] = None,
  extraSamples: Option[Vector[Int]] = None,
  fillOrder: Option[Int] = None,
  freeByteCounts: Option[Vector[Int]] = None,
  freeOffsets: Option[Vector[Int]] = None,
  grayResponseCurve: Option[Vector[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Int] = None,
  orientation: Option[Int] = None,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Option[Int] = None,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Vector[Int]] = None,
  predictor: Option[Int] = None
)

case class GeoTiffTags(
  modelTiePoints: Option[Vector[ModelTiePoint]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Vector[Double]] = None,
  asciis: Option[String] = None
)

case class DocumentationTags(
  documentNames: Option[String] = None,
  pageNames: Option[String] = None,
  pageNumbers: Option[Vector[Int]] = None,
  xPositions: Option[Vector[(Int, Int)]] = None,
  yPositions: Option[Vector[(Int, Int)]] = None
)

case class TileTags(
  tileWidth: Option[Int] = None,
  tileLength: Option[Int] = None,
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
  maxSampleValues: Option[Vector[Int]] = None,
  minSampleValues: Option[Vector[Int]] = None
)

case class ColimetryTags(
  whitePoints: Option[Vector[(Int, Int)]] = None,
  primaryChromaticities: Option[Vector[(Int, Int)]] = None,
  transferFunction: Option[Vector[Int]] = None,
  transferRange: Option[Vector[Int]] = None,
  referenceBlackWhite: Option[Vector[Int]] = None
)

case class JpegTags(
  jpegProc: Option[Int] = None,
  jpegInterchangeFormat: Option[Int] = None,
  jpegInterchangeFormatLength: Option[Int] = None,
  jpegRestartInterval: Option[Int] = None,
  jpegLosslessPredictors: Option[Vector[Int]] = None,
  jpegPointTransforms: Option[Vector[Int]] = None,
  jpegQTables: Option[Vector[Int]] = None,
  jpegDCTables: Option[Vector[Int]] = None,
  jpegACTables: Option[Vector[Int]] = None
)

case class YCbCrTags(
  yCbCrCoefficients: Option[Vector[(Int, Int)]] = None,
  yCbCrSubSampling: Option[Vector[Int]] = None,
  yCbCrPositioning: Option[Int] = None
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
    Option[Int]]("imageLength")
  val imageWidthLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Int]]("imageWidth")
  val compressionLens = basicTagsLens |-> mkLens[BasicTags,
    Int]("compression")
  val photometricInterpLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Int]]("photometricInterp")
  val resolutionUnitLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Int]]("resolutionUnit")
  val rowsPerStripLens = basicTagsLens |-> mkLens[BasicTags,
    Int]("rowsPerStrip")
  val samplesPerPixelLens = basicTagsLens |-> mkLens[BasicTags,
    Int]("samplesPerPixel")
  val stripByteCountsLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Vector[Int]]]("stripByteCounts")
  val stripOffsetsLens = basicTagsLens |-> mkLens[BasicTags,
    Option[Vector[Int]]]("stripOffsets")
  val xResolutionLens = basicTagsLens |-> mkLens[BasicTags,
    Option[(Int, Int)]]("xResolution")
  val yResolutionLens = basicTagsLens |-> mkLens[BasicTags,
    Option[(Int, Int)]]("yResolution")

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
    Option[Vector[Int]]]("freeByteCounts")
  val freeOffsetsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Vector[Int]]]("freeOffsets")
  val grayResponseCurveLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Vector[Int]]]("grayResponseCurve")
  val grayResponseUnitLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("grayResponseUnit")
  val newSubfileTypeLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("newSubfileType")
  val orientationLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("orientation")
  val planarConfigurationLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("planarConfiguration")
  val subfileTypeLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("subfileType")
  val thresholdingLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Int]("thresholding")
  val t4OptionsLens = nonBasicTagsLens |-> mkLens[NonBasicTags,
    Option[Int]]("t4Options")
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
  val geoKeyDirectoryLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[GeoKeyDirectory]]("geoKeyDirectory")
  val doublesLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[Vector[Double]]]("doubles")
  val asciisLens = geoTiffTagsLens |-> mkLens[GeoTiffTags,
    Option[String]]("asciis")

  val documentationTagsLens = mkLens[ImageDirectory,
    DocumentationTags]("documentationTags")

  val documentNamesLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[String]]("documentNames")
  val pageNamesLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[String]]("pageNames")
  val pageNumbersLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[Int]]]("pageNumbers")
  val xPositionsLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[(Int, Int)]]]("xPositions")
  val yPositionsLens = documentationTagsLens |->
  mkLens[DocumentationTags, Option[Vector[(Int, Int)]]]("yPositions")

  val tileTagsLens = mkLens[ImageDirectory, TileTags]("tileTags")

  val tileWidthLens = tileTagsLens |-> mkLens[TileTags,
    Option[Int]]("tileWidth")
  val tileLengthLens = tileTagsLens |-> mkLens[TileTags,
    Option[Int]]("tileLength")
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
  val maxSampleValuesLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Int]]]("maxSampleValues")
  val minSampleValuesLens = dataSampleFormatTagsLens |->
  mkLens[DataSampleFormatTags, Option[Vector[Int]]]("minSampleValues")

  val colimetryTagsLens = mkLens[ImageDirectory, ColimetryTags]("colimetryTags")

  val whitePointsLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Vector[(Int, Int)]]]("whitePoints")
  val primaryChromaticitiesLens = colimetryTagsLens |-> mkLens[
    ColimetryTags, Option[Vector[(Int, Int)]]]("primaryChromaticities")
  val transferFunctionLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Vector[Int]]]("transferFunction")
  val transferRangeLens = colimetryTagsLens |-> mkLens[ColimetryTags,
    Option[Vector[Int]]]("transferRange")
  val referenceBlackWhiteLens = colimetryTagsLens |-> mkLens[
    ColimetryTags,  Option[Vector[Int]]]("referenceBlackWhite")

  val jpegTagsLens = mkLens[ImageDirectory, JpegTags]("jpegTags")

  val jpegProcLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegProc")
  val jpegInterchangeFormatLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegInterchangeFormat")
  val jpegInterchangeFormatLengthLens = jpegTagsLens |-> mkLens[
    JpegTags, Option[Int]]("jpegInterchangeFormatLength")
  val jpegRestartIntervalLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Int]]("jpegRestartInterval")
  val jpegLosslessPredictorsLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegLosslessPredictors")
  val jpegPointTransformsLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegPointTransforms")
  val jpegQTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegQTables")
  val jpegDCTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegDCTables")
  val jpegACTablesLens = jpegTagsLens |-> mkLens[JpegTags,
    Option[Vector[Int]]]("jpegACTables")

  val yCbCrTagsLens = mkLens[ImageDirectory, YCbCrTags]("yCbCrTags")

  val yCbCrCoefficientsLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Vector[(Int, Int)]]]("yCbCrCoefficients")
  val yCbCrSubSamplingLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Vector[Int]]]("yCbCrSubSampling")
  val yCbCrPositioningLens = yCbCrTagsLens |-> mkLens[YCbCrTags,
    Option[Int]]("yCbCrPositioning")

  val imageBytesLens = mkLens[ImageDirectory,
    Option[Vector[Byte]]]("imageBytes")

}

/*object ImageDirectoryLenses {

 val countLens = lens[ImageDirectory] >> 'count

 val metaDataTagsLens = lens[ImageDirectory] >> 'metadataTags

 val artistLens = metaDataTagsLens >> 'artist
 val copyrightLens = metaDataTagsLens >> 'copyright
 val dateTimeLens = metaDataTagsLens >> 'dateTime
 val computerLens = metaDataTagsLens >> 'computer
 val imageDescLens = metaDataTagsLens >> 'imageDesc
 val makerLens = metaDataTagsLens >> 'maker
 val modelLens = metaDataTagsLens >> 'model
 val softwareLens = metaDataTagsLens >> 'software

 val basicTagsLens = lens[ImageDirectory] >> 'basicTags

 val bitsPerSampleLens = basicTagsLens >> 'bitsPerSample
 val colorMapLens = basicTagsLens >> 'colorMap
 val imageLengthLens = basicTagsLens >> 'imageLength
 val imageWidthLens = basicTagsLens >> 'imageWidth
 val compressionLens = basicTagsLens >> 'compression
 val photometricInterpLens = basicTagsLens >> 'photometricInterp
 val resolutionUnitLens = basicTagsLens >> 'resolutionUnit
 val rowsPerStripLens = basicTagsLens >> 'rowsPerStrip
 val samplesPerPixelLens = basicTagsLens >> 'samplesPerPixel
 val stripByteCountsLens = basicTagsLens >> 'stripByteCounts
 val stripOffsetsLens = basicTagsLens >> 'stripOffsets
 val xResolutionLens = basicTagsLens >> 'xResolution
 val yResolutionLens = basicTagsLens >> 'yResolution

 val nonBasicTagsLens = lens[ImageDirectory] >> 'nonBasicTags

 val cellLengthLens = nonBasicTagsLens >> 'cellLength
 val cellWidthLens = nonBasicTagsLens >> 'cellWidth
 val extraSamplesLens = nonBasicTagsLens >> 'extraSamples
 val fillOrderLens = nonBasicTagsLens >> 'fillOrder
 val freeByteCountsLens = nonBasicTagsLens >> 'freeByteCounts
 val freeOffsetsLens = nonBasicTagsLens >> 'freeOffsets
 val grayResponseCurveLens = nonBasicTagsLens >> 'grayResponseCurve
 val grayResponseUnitLens = nonBasicTagsLens >> 'grayResponseUnit
 val newSubfileTypeLens = nonBasicTagsLens >> 'newSubfileType
 val orientationLens = nonBasicTagsLens >> 'orientation
 val planarConfigurationLens = nonBasicTagsLens >> 'planarConfiguration
 val subfileTypeLens = nonBasicTagsLens >> 'subfileType
 val thresholdingLens = nonBasicTagsLens >> 'thresholding
 val t4OptionsLens = nonBasicTagsLens >> 't4Options
 val t6OptionsLens = nonBasicTagsLens >> 't6Options
 val halftoneHintsLens = nonBasicTagsLens >> 'halftoneHints
 val predictorLens = nonBasicTagsLens >> 'predictor

 val geoTiffTagsLens = lens[ImageDirectory] >> 'geoTiffTags

 val modelTiePointsLens = geoTiffTagsLens >> 'modelTiePoints
 val modePixelScaleLens = geoTiffTagsLens >> 'modelPixelScale
 val geoKeyDirectoryLens = geoTiffTagsLens >> 'geoKeyDirectory
 val doublesLens = geoTiffTagsLens >> 'doubles
 val asciisLens = geoTiffTagsLens >> 'asciis

 val documentationTagsLens = lens[ImageDirectory] >> 'documentationTags

 val documentNamesLens = documentationTagsLens >> 'documentNames
 val pageNamesLens = documentationTagsLens >> 'pageNames
 val pageNumbersLens = documentationTagsLens >> 'pageNumbers
 val xPositionsLens = documentationTagsLens >> 'xPositions
 val yPositionsLens = documentationTagsLens >> 'yPositions

 val tileTagsLens = lens[ImageDirectory] >> 'tileTags

 val tileWidthLens = tileTagsLens >> 'tileWidth
 val tileLengthLens = tileTagsLens >> 'tileLength
 val tileOffsetsLens = tileTagsLens >> 'tileOffsets
 val tileByteCountsLens = tileTagsLens >> 'tileByteCounts

 val cmykTagsLens = lens[ImageDirectory] >> 'cmykTags

 val inkSetLens = cmykTagsLens >> 'inkSet
 val numberOfInksLens = cmykTagsLens >> 'numberOfInks
 val inkNamesLens = cmykTagsLens >> 'inkNames
 val dotRangeLens = cmykTagsLens >> 'dotRange
 val targetPrintersLens = cmykTagsLens >> 'targetPrinters

 val dataSampleFormatTagsLens = lens[ImageDirectory] >> 'dataSampleFormatTags

 val sampleFormatLens = dataSampleFormatTagsLens >> 'sampleFormat
 val maxSampleValuesLens = dataSampleFormatTagsLens >> 'maxSampleValues
 val minSampleValuesLens = dataSampleFormatTagsLens >> 'minSampleValues

 val colimetryTagsLens = lens[ImageDirectory] >> 'colimetryTags

 val whitePointsLens = colimetryTagsLens >> 'whitePoints
 val primaryChromaticitiesLens = colimetryTagsLens >> 'primaryChromaticities
 val transferFunctionLens = colimetryTagsLens >> 'transferFunction
 val transferRangeLens = colimetryTagsLens >> 'transferRange
 val referenceBlackWhiteLens = colimetryTagsLens >> 'referenceBlackWhite

 val jpegTagsLens = lens[ImageDirectory] >> 'jpegTags

 val jpegProcLens = jpegTagsLens >> 'jpegProc
 val jpegInterchangeFormatLens = jpegTagsLens >> 'jpegInterchangeFormat
 val jpegInterchangeFormatLengthLens =
 jpegTagsLens >> 'jpegInterchangeFormatLength
 val jpegRestartIntervalLens = jpegTagsLens >> 'jpegRestartInterval
 val jpegLosslessPredictorsLens = jpegTagsLens >> 'jpegLosslessPredictors
 val jpegPointTransformsLens = jpegTagsLens >> 'jpegPointTransforms
 val jpegQTablesLens = jpegTagsLens >> 'jpegQTables
 val jpegDCTablesLens = jpegTagsLens >> 'jpegDCTables
 val jpegACTablesLens = jpegTagsLens >> 'jpegACTables

 val yCbCrTagsLens = lens[ImageDirectory] >> 'yCbCrTags

 val yCbCrCoefficientsLens = yCbCrTagsLens >> 'yCbCrCoefficients
 val yCbCrSubSamplingLens = yCbCrTagsLens >> 'yCbCrSubSampling
 val yCbCrPositioningLens = yCbCrTagsLens >> 'yCbCrPositioning

 val imageBytesLens = lens[ImageDirectory] >> 'imageBytes

 }*/
