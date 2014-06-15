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

import scala.math.BigInt

case class TagMetadata(tag: Int, fieldType: Int,
  length: Int, offset: Int)

case class ModelTiePoint(i: Double, j: Double, k: Double,
  x: Double, y: Double, z:Double)

case class IFDMetadata(
  artist: Option[String] = None,
  copyright: Option[String] = None,
  dateTime: Option[String] = None, //ask rob about datetime, joda?
  computer: Option[String] = None,
  imageDesc: Option[String] = None,
  maker: Option[String] = None,
  model: Option[String] = None,
  software: Option[String] = None
)

case class IFDBasics(
  bitsPerSample: Array[Int] = Array(1),
  colorMap: Option[Array[Int]] = None,
  imageLength: Option[Int] = None,
  imageWidth: Option[Int] = None,
  compression: Int = 1,
  photometricInterp: Option[Int] = None,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Int = (1 << 31) - 1,
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Array[Int]] = None,
  stripOffsets: Option[Array[Int]] = None,
  xResolution: Option[(Int, Int)] = None,
  yResolution: Option[(Int, Int)] = None
)

case class IFDGeoTiffTags(
  modelTiePoints: Option[Array[ModelTiePoint]] = None,
  modelPixelScale: Option[(Double, Double, Double)] = None,
  geoKeyDirectory: Option[GeoKeyDirectory] = None,
  doubles: Option[Array[Double]] = None,
  asciis: Option[String] = None
)

case class IFDNonBasics(
  cellLength: Option[Int] = None,
  cellWidth: Option[Int] = None,
  extraSamples: Option[Array[Int]] = None,
  fillOrder: Option[Int] = None,
  freeByteCounts: Option[Array[Int]] = None,
  freeOffsets: Option[Array[Int]] = None,
  grayResponseCurve: Option[Array[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Int] = None,
  orientation: Option[Int] = None,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Option[Int] = None,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Array[Int]] = None,
  predictor: Option[Int] = None
)

case class IFDDocumentationTags(
  documentNames: Option[String] = None,
  pageNames: Option[String] = None,
  pageNumbers: Option[Array[Int]] = None,
  xPositions: Option[Array[(Int, Int)]] = None,
  yPositions: Option[Array[(Int, Int)]] = None
)

case class IFDTilesTags(
  tileWidth: Option[Int] = None,
  tileLength: Option[Int] = None,
  tileOffsets: Option[Array[Int]] = None,
  tileByteCounts: Option[Array[Int]] = None
)

case class IFDCMYKTags(
  inkSet: Option[Int] = None,
  numberOfInks: Option[Int] = None,
  inkNames: Option[String] = None,
  dotRange: Option[Array[Int]] = None,
  targetPrinters: Option[String] = None
)

case class IFDDataSampleFormatTags(
  sampleFormat: Option[Array[Int]] = None,
  maxSampleValues: Option[Array[Int]] = None,
  minSampleValues: Option[Array[Int]] = None
)

case class IFDColimetryTags(
  whitePoints: Option[Array[(Int, Int)]] = None,
  primaryChromaticities: Option[Array[(Int, Int)]] = None,
  transferFunction: Option[Array[Int]] = None,
  transferRange: Option[Array[Int]] = None,
  referenceBlackWhite: Option[Array[Int]] = None
)

case class IFDJPEGTags(
  jpegProc: Option[Int] = None,
  jpegInterchangeFormat: Option[Int] = None,
  jpegInterchangeFormatLength: Option[Int] = None,
  jpegRestartInterval: Option[Int] = None,
  jpegLosslessPredictors: Option[Array[Int]] = None,
  jpegPointTransforms: Option[Array[Int]] = None,
  jpegQTables: Option[Array[Int]] = None,
  jpegDCTables: Option[Array[Int]] = None,
  jpegACTables: Option[Array[Int]] = None
)

case class IFDYCbCrTags(
  yCbCrCoefficients: Option[Array[(Int, Int)]] = None,
  yCbCrSubSampling: Option[Array[Int]] = None,
  yCbCrPositioning: Option[Int] = None
)

case class IFDTags(
  count: Int,
  metadata: IFDMetadata = IFDMetadata(),
  basics: IFDBasics = IFDBasics(),
  nonBasics: IFDNonBasics = IFDNonBasics(),
  documentationTags: IFDDocumentationTags = IFDDocumentationTags(),
  tilesTags: IFDTilesTags = IFDTilesTags(),
  cmykTags: IFDCMYKTags = IFDCMYKTags(),
  dataSampleFormatTags: IFDDataSampleFormatTags = IFDDataSampleFormatTags(),
  colimetryTags: IFDColimetryTags = IFDColimetryTags(),
  jpegTags: IFDJPEGTags = IFDJPEGTags(),
  yCbCrTags: IFDYCbCrTags = IFDYCbCrTags(),
  geoTiffTags: IFDGeoTiffTags = IFDGeoTiffTags(),
  imageBytes: Option[Array[Char]] = None
)

case class IFD(tags: IFDTags)
