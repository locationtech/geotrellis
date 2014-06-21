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

import java.nio.ByteBuffer

import geotrellis.io.geotiff.utils.ByteBufferUtils._

import geotrellis.io.geotiff.ImageDirectoryLenses._

import monocle.syntax._

case class TagReader(byteBuffer: ByteBuffer) {

  def read(directory: ImageDirectory, tagMetadata: TagMetadata):
      ImageDirectory = (tagMetadata.tag, tagMetadata.fieldType,
        tagMetadata.length) match {
    case (33550, _, _) => readModelPixelScaleTag(directory, tagMetadata)
    case (33922, _, _) => readModelTiePointsTag(directory, tagMetadata)
    case (34735, _, _) => readGeoKeyDirectoryTag(directory, tagMetadata)
    case (_, 1, _) => readBytesTag(directory, tagMetadata)
    case (_, 2, _) => readAsciisTag(directory, tagMetadata)
    case (_, 3, _) => readShortsTag(directory, tagMetadata)
    case (_, 4, _) => readIntsTag(directory, tagMetadata)
    case (_, 5, _) => readFractionalsTag(directory, tagMetadata)
    case (_, 12, _) => readDoublesTag(directory, tagMetadata)
  }

  private def readModelPixelScaleTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val oldPos = byteBuffer.position

    byteBuffer.position(tagMetadata.offset)

    val scaleX = byteBuffer.getDouble
    val scaleY = byteBuffer.getDouble
    val scaleZ = byteBuffer.getDouble

    byteBuffer.position(oldPos)

    directory |-> modelPixelScaleLens set(Some(scaleX, scaleY,
      scaleZ))
  }

  private def readModelTiePointsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val oldPos = byteBuffer.position

    val numberOfPoints = tagMetadata.length / 6

    byteBuffer.position(tagMetadata.offset)

    val points = (for (i <- 0 until numberOfPoints) yield ModelTiePoint(
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble
    )).toVector

    byteBuffer.position(oldPos)

    directory |-> modelTiePointsLens set(Some(points))
  }

  private def readGeoKeyDirectoryTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val oldPos = byteBuffer.position

    byteBuffer.position(tagMetadata.offset)

    val version = byteBuffer.getShort
    val keyRevision = byteBuffer.getShort
    val minorRevision = byteBuffer.getShort
    val numberOfKeys = byteBuffer.getShort

    val keyDirectoryMetadata = KeyDirectoryMetadata(version, keyRevision,
      minorRevision, numberOfKeys)

    val keyDirectoryReader = KeyDirectoryReader(byteBuffer, directory)

    val geoKeyDirectory = keyDirectoryReader.read(GeoKeyDirectory(count =
      numberOfKeys))

    byteBuffer.position(oldPos)

    directory |-> geoKeyDirectoryLens set(Some(geoKeyDirectory))
  }

  private def readBytesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val bytes = byteBuffer.getByteVector(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case 336 => directory |-> dotRangeLens set(Some(bytes))
      case 338 => directory |-> extraSamplesLens set(Some(bytes))
    }
  }

  private def readAsciisTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val string = byteBuffer.getString(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case 270 => directory |-> imageDescLens set(Some(string))
      case 271 => directory |-> makerLens set(Some(string))
      case 272 => directory |-> modelLens set(Some(string))
      case 305 => directory |-> softwareLens set(Some(string))
      case 315 => directory |-> artistLens set(Some(string))
      case 316 => directory |-> computerLens set(Some(string))
      case 33432 => directory |-> copyrightLens set(Some(string))
      case 34737 => directory |-> asciisLens set(Some(string))
    }
  }

  private def readShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getShortVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case 255 => directory |-> subfileTypeLens set(Some(shorts(0)))
      case 256 => directory |-> imageWidthLens set(Some(shorts(0)))
      case 257 => directory |-> imageLengthLens set(Some(shorts(0)))
      case 259 => directory |-> compressionLens set(shorts(0))
      case 262 => directory |-> photometricInterpLens set(Some(shorts(0)))
      case 263 => directory |-> thresholdingLens set(shorts(0))
      case 264 => directory |-> cellWidthLens set(Some(shorts(0)))
      case 265 => directory |-> cellLengthLens set(Some(shorts(0)))
      case 266 => directory |-> fillOrderLens set(Some(shorts(0)))
      case 274 => directory |-> orientationLens set(Some(shorts(0)))
      case 277 => directory |-> samplesPerPixelLens set(shorts(0))
      case 278 => directory |-> rowsPerStripLens set(shorts(0))
      case 284 => directory |-> planarConfigurationLens set(Some(shorts(0)))
      case 290 => directory |-> grayResponseUnitLens set(Some(shorts(0)))
      case 296 => directory |-> resolutionUnitLens set(Some(shorts(0)))
      case 317 => directory |-> predictorLens set(Some(shorts(0)))
      case 322 => directory |-> tileWidthLens set(Some(shorts(0)))
      case 323 => directory |-> tileLengthLens set(Some(shorts(0)))
      case 332 => directory |-> inkSetLens set(Some(shorts(0)))
      case 334 => directory |-> numberOfInksLens set(Some(shorts(0)))
      case 512 => directory |-> jpegProcLens set(Some(shorts(0)))
      case 513 => directory |-> jpegInterchangeFormatLens set(Some(shorts(0)))
      case 514 => directory |-> jpegInterchangeFormatLengthLens set(
        Some(shorts(0)))
      case 515 => directory |-> jpegRestartIntervalLens set(Some(shorts(0)))
      case 531 => directory |-> yCbCrPositioningLens set(Some(shorts(0)))
      case 258 => directory |-> bitsPerSampleLens set(shorts)
      case 273 => directory |-> stripOffsetsLens set(Some(shorts))
      case 279 => directory |-> stripByteCountsLens set(Some(shorts))
      case 280 => directory |-> minSampleValuesLens set(Some(shorts))
      case 281 => directory |-> maxSampleValuesLens set(Some(shorts))
      case 291 => directory |-> grayResponseCurveLens set(Some(shorts))
      case 297 => directory |-> pageNumbersLens set(Some(shorts))
      case 301 => directory |-> transferFunctionLens set(Some(shorts))
      case 320 => directory |-> colorMapLens set(Some(shorts))
      case 321 => directory |-> halftoneHintsLens set(Some(shorts))
      case 325 => directory |-> tileByteCountsLens set(Some(shorts))
      case 336 => directory |-> dotRangeLens set(Some(shorts))
      case 339 => directory |-> sampleFormatLens set(Some(shorts))
      case 342 => directory |-> transferRangeLens set(Some(shorts))
      case 517 => directory |-> jpegLosslessPredictorsLens set(Some(shorts))
      case 518 => directory |-> jpegPointTransformsLens set(Some(shorts))
    }
  }

  private def readIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getIntVector(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case 254 => directory |-> newSubfileTypeLens set(Some(ints(0)))
      case 256 => directory |-> imageWidthLens set(Some(ints(0)))
      case 257 => directory |-> imageLengthLens set(Some(ints(0)))
      case 292 => directory |-> t4OptionsLens set(Some(ints(0)))
      case 293 => directory |-> t6OptionsLens set(Some(ints(0)))
      case 322 => directory |-> tileWidthLens set(Some(ints(0)))
      case 323 => directory |-> tileLengthLens set(Some(ints(0)))
      case 513 => directory |-> jpegInterchangeFormatLens set(Some(ints(0)))
      case 514 => directory |-> jpegInterchangeFormatLengthLens set(
        Some(ints(0)))
      case 273 => directory |-> stripOffsetsLens set(Some(ints))
      case 279 => directory |-> stripByteCountsLens set(Some(ints))
      case 288 => directory |-> freeOffsetsLens set(Some(ints))
      case 289 => directory |-> freeByteCountsLens set(Some(ints))
      case 324 => directory |-> tileOffsetsLens set(Some(ints))
      case 325 => directory |-> tileByteCountsLens set(Some(ints))
      case 519 => directory |-> jpegQTablesLens set(Some(ints))
      case 520 => directory |-> jpegDCTablesLens set(Some(ints))
      case 521 => directory |-> jpegACTablesLens set(Some(ints))
      case 532 => directory |-> referenceBlackWhiteLens set(Some(ints))
    }
  }

  private def readFractionalsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getFractionalVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case 282 => directory |-> xResolutionLens set(Some(fractionals(0)))
      case 283 => directory |-> yResolutionLens set(Some(fractionals(0)))
      case 286 => directory |-> xPositionsLens set(Some(fractionals))
      case 287 => directory |-> yPositionsLens set(Some(fractionals))
      case 318 => directory |-> whitePointsLens set(Some(fractionals))
      case 319 => directory |-> primaryChromaticitiesLens set(Some(fractionals))
      case 529 => directory |-> yCbCrCoefficientsLens set(Some(fractionals))
    }
  }

  private def readDoublesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val doubles = byteBuffer.getDoubleVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case 34736 => directory |-> doublesLens set(Some(doubles))
    }
  }

}
