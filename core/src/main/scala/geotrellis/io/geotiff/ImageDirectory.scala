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

//import shapeless._

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
  bitsPerSample: Vector[Int] = Vector(1),
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

case class GeoTiffTags(
  modelTiePoints: Option[Vector[ModelTiePoint]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Vector[Double]] = None,
  asciis: Option[String] = None
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
  documentationTags: DocumentationTags = DocumentationTags(),
  tileTags: TileTags = TileTags(),
  cmykTags: CmykTags = CmykTags(),
  dataSampleFormatTags: DataSampleFormatTags = DataSampleFormatTags(),
  colimetryTags: ColimetryTags = ColimetryTags(),
  jpegTags: JpegTags = JpegTags(),
  yCbCrTags: YCbCrTags = YCbCrTags(),
  geoTiffTags: GeoTiffTags = GeoTiffTags(),
  imageBytes: Option[Vector[Byte]] = None
)
