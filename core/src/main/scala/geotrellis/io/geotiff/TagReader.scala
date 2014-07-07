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

import geotrellis.io.geotiff.Tags._
import geotrellis.io.geotiff.TiffFieldType._

import monocle.syntax._

case class TagReader(byteBuffer: ByteBuffer) {

  // unapply? maybz + enums instead of raw ints
  def read(directory: ImageDirectory, tagMetadata: TagMetadata):
      ImageDirectory = (tagMetadata.tag, tagMetadata.fieldType) match {
    case (ModelPixelScaleTag, _) =>
      readModelPixelScaleTag(directory, tagMetadata)
    case (ModelTiePointsTag, _) => readModelTiePointsTag(directory, tagMetadata)
    case (GeoKeyDirectoryTag, _) =>
      readGeoKeyDirectoryTag(directory, tagMetadata)
    case (_, BytesFieldType) => readBytesTag(directory, tagMetadata)
    case (_, AsciisFieldType) => readAsciisTag(directory, tagMetadata)
    case (_, ShortsFieldType) => readShortsTag(directory, tagMetadata)
    case (_, IntsFieldType) => readIntsTag(directory, tagMetadata)
    case (_, FractionalsFieldType) => readFractionalsTag(directory, tagMetadata)
    case (_, SignedBytesFieldType) => readSignedBytesTag(directory, tagMetadata)
    case (_, UndefinedFieldType) => readUndefinedTag(directory, tagMetadata)
    case (_, SignedShortsFieldType) =>
      readSignedShortsTag(directory, tagMetadata)
    case (_, SignedIntsFieldType) => readSignedIntsTag(directory, tagMetadata)
    case (_, SignedFractionalsFieldType) =>
      readSignedFractionalsTag(directory, tagMetadata)
    case (_, FloatsFieldType) => readFloatsTag(directory, tagMetadata)
    case (_, DoublesFieldType) => readDoublesTag(directory, tagMetadata)
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

    val keyDirectoryMetadata = GeoKeyDirectoryMetadata(version, keyRevision,
      minorRevision, numberOfKeys)

    val geoKeyReader = GeoKeyReader(byteBuffer, directory)

    val geoKeyDirectory = geoKeyReader.read(GeoKeyDirectory(count =
      numberOfKeys))

    byteBuffer.position(oldPos)

    directory |-> geoKeyDirectoryLens set(Some(geoKeyDirectory))
  }

  private def readBytesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val bytes = byteBuffer.getByteVector(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case DotRangeTag =>
        directory |-> dotRangeLens set(Some(bytes.map(_.toInt)))
      case ExtraSamplesTag =>
        directory |-> extraSamplesLens set(Some(bytes.map(_.toInt)))
      case tag => directory |-> longsMapLens modify(_ + (tag ->
          bytes.map(_.toLong)))
    }
  }

  private def readAsciisTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val string = byteBuffer.getString(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case ImageDescTag => directory |-> imageDescLens set(Some(string))
      case MakerTag => directory |-> makerLens set(Some(string))
      case ModelTag => directory |-> modelLens set(Some(string))
      case SoftwareTag => directory |-> softwareLens set(Some(string))
      case ArtistTag => directory |-> artistLens set(Some(string))
      case HostComputerTag => directory |-> hostComputerLens set(Some(string))
      case CopyrightTag => directory |-> copyrightLens set(Some(string))
      case AsciisTag => directory |-> asciisLens set(Some(string))
      case tag => directory |-> asciisMapLens modify(_ + (tag -> string))
    }
  }

  private def readShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getShortVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case SubfileTypeTag => directory |-> subfileTypeLens set(Some(shorts(0)))
      case ImageWidthTag => directory |-> imageWidthLens set(shorts(0))
      case ImageLengthTag => directory |-> imageLengthLens set(shorts(0))
      case CompressionTag => directory |-> compressionLens set(shorts(0))
      case PhotometricInterpTag =>
        directory |-> photometricInterpLens set(Some(shorts(0)))
      case ThresholdingTag => directory |-> thresholdingLens set(shorts(0))
      case CellWidthTag => directory |-> cellWidthLens set(Some(shorts(0)))
      case CellLengthTag => directory |-> cellLengthLens set(Some(shorts(0)))
      case FillOrderTag => directory |-> fillOrderLens set(Some(shorts(0)))
      case OrientationTag => directory |-> orientationLens set(Some(shorts(0)))
      case SamplesPerPixelTag =>
        directory |-> samplesPerPixelLens set(shorts(0))
      case RowsPerStripTag => directory |-> rowsPerStripLens set(shorts(0))
      case PlanarConfigurationTag =>
        directory |-> planarConfigurationLens set(Some(shorts(0)))
      case GrayResponseUnitTag =>
        directory |-> grayResponseUnitLens set(Some(shorts(0)))
      case ResolutionUnitTag =>
        directory |-> resolutionUnitLens set(Some(shorts(0)))
      case PredictorTag => directory |-> predictorLens set(Some(shorts(0)))
      case TileWidthTag => directory |-> tileWidthLens set(Some(shorts(0)))
      case TileLengthTag => directory |-> tileLengthLens set(Some(shorts(0)))
      case InkSetTag => directory |-> inkSetLens set(Some(shorts(0)))
      case NumberOfInksTag =>
        directory |-> numberOfInksLens set(Some(shorts(0)))
      case JpegProcTag => directory |-> jpegProcLens set(Some(shorts(0)))
      case JpegInterchangeFormatTag =>
        directory |-> jpegInterchangeFormatLens set(Some(shorts(0)))
      case JpegInterchangeFormatLengthTag =>
        directory |-> jpegInterchangeFormatLengthLens set(
        Some(shorts(0)))
      case JpegRestartIntervalTag =>
        directory |-> jpegRestartIntervalLens set(Some(shorts(0)))
      case YCbCrPositioningTag =>
        directory |-> yCbCrPositioningLens set(Some(shorts(0)))
      case BitsPerSampleTag => directory |-> bitsPerSampleLens set(Some(shorts))
      case StripOffsetsTag => directory |-> stripOffsetsLens set(Some(shorts))
      case StripByteCountsTag => directory |-> stripByteCountsLens set(
        Some(shorts))
      case MinSampleValueTag => directory |-> minSampleValueLens set(Some(
        shorts.map(_.toLong)))
      case MaxSampleValueTag => directory |-> maxSampleValueLens set(Some(
        shorts.map(_.toLong)))
      case GrayResponseCurveTag =>
        directory |-> grayResponseCurveLens set(Some(shorts))
      case PageNumberTag => directory |-> pageNumberLens set(Some(shorts))
      case TransferFunctionTag =>
        directory |-> transferFunctionLens set(Some(shorts))
      case ColorMapTag => directory |-> colorMapLens set(Some(shorts))
      case HalftoneHintsTag => directory |-> halftoneHintsLens set(Some(shorts))
      case TileByteCountsTag => directory |-> tileByteCountsLens set(
        Some(shorts))
      case DotRangeTag => directory |-> dotRangeLens set(Some(shorts))
      case SampleFormatTag =>
        directory |-> sampleFormatLens set(Some(shorts))
      case TransferRangeTag =>
        directory |-> transferRangeLens set(Some(shorts))
      case JpegLosslessPredictorsTag =>
        directory |-> jpegLosslessPredictorsLens set(Some(shorts))
      case JpegPointTransformsTag =>
        directory |-> jpegPointTransformsLens set(Some(shorts))
      case tag => directory |-> longsMapLens modify(_ + (tag ->
          shorts.map(_.toLong)))
    }
  }

  private def readIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getIntVector(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case NewSubfileTypeTag =>
        directory |-> newSubfileTypeLens set(Some(ints(0)))
      case ImageWidthTag => directory |-> imageWidthLens set(ints(0))
      case ImageLengthTag => directory |-> imageLengthLens set(ints(0))
      case T4OptionsTag => directory |-> t4OptionsLens set(ints(0).toInt)
      case T6OptionsTag => directory |-> t6OptionsLens set(Some(ints(0).toInt))
      case TileWidthTag => directory |-> tileWidthLens set(Some(ints(0)))
      case TileLengthTag => directory |-> tileLengthLens set(Some(ints(0)))
      case JpegInterchangeFormatTag =>
        directory |-> jpegInterchangeFormatLens set(Some(ints(0)))
      case JpegInterchangeFormatLengthTag =>
        directory |-> jpegInterchangeFormatLengthLens set(
        Some(ints(0)))
      case StripOffsetsTag =>
        directory |-> stripOffsetsLens set(Some(ints.map(_.toInt)))
      case StripByteCountsTag =>
        directory |-> stripByteCountsLens set(Some(ints.map(_.toInt)))
      case FreeOffsetsTag => directory |-> freeOffsetsLens set(Some(ints))
      case FreeByteCountsTag => directory |-> freeByteCountsLens set(Some(ints))
      case TileOffsetsTag => directory |-> tileOffsetsLens set(
        Some(ints.map(_.toInt)))
      case TileByteCountsTag => directory |-> tileByteCountsLens set(
        Some(ints.map(_.toInt)))
      case JpegQTablesTag => directory |-> jpegQTablesLens set(Some(ints))
      case JpegDCTablesTag => directory |-> jpegDCTablesLens set(Some(ints))
      case JpegACTablesTag => directory |-> jpegACTablesLens set(Some(ints))
      case ReferenceBlackWhiteTag =>
        directory |-> referenceBlackWhiteLens set(Some(ints))
      case tag => directory |-> longsMapLens modify(_ + (tag -> ints))
    }
  }

  private def readFractionalsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getFractionalVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case XResolutionTag =>
        directory |-> xResolutionLens set(Some(fractionals(0)))
      case YResolutionTag =>
        directory |-> yResolutionLens set(Some(fractionals(0)))
      case XPositionTag => directory |-> xPositionsLens set(Some(fractionals))
      case YPositionTag => directory |-> yPositionsLens set(Some(fractionals))
      case WhitePointTag => directory |-> whitePointsLens set(Some(fractionals))
      case PrimaryChromaticitiesTag =>
        directory |-> primaryChromaticitiesLens set(Some(fractionals))
      case YCbCrCoefficientsTag =>
        directory |-> yCbCrCoefficientsLens set(Some(fractionals))
      case tag => directory |-> fractionalsMapLens modify(_ + (tag ->
          fractionals))
    }
  }

  private def readSignedBytesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val bytes = byteBuffer.getSignedByteVector(tagMetadata.length,
      tagMetadata.offset)

    directory |-> longsMapLens modify (_ + (tagMetadata.tag
      -> bytes.map(_.toLong)))
  }

  private def readUndefinedTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val bytes = byteBuffer.getSignedByteVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case JpegTablesTag => directory |-> jpegTablesLens set(Some(bytes))
      case tag => directory |-> undefinedMapLens modify (_ + (tag -> bytes))
    }

  }

  private def readSignedShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getSignedShortVector(tagMetadata.length,
      tagMetadata.offset)

    directory |-> longsMapLens modify(_ + (tagMetadata.tag
      -> shorts.map(_.toLong)))
  }

  private def readSignedIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getSignedIntVector(tagMetadata.length,
      tagMetadata.offset)

    directory |-> longsMapLens modify(_ + (tagMetadata.tag ->
      ints.map(_.toLong)))
  }

  private def readSignedFractionalsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getSignedFractionalVector(tagMetadata.length,
      tagMetadata.offset)

    directory |-> fractionalsMapLens modify(_ + (tagMetadata.tag
      -> fractionals.map(x => (x._1.toLong, x._2.toLong))))
  }

  private def readFloatsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val floats = byteBuffer.getFloatVector(tagMetadata.length,
      tagMetadata.offset)

    directory |-> doublesMapLens modify(_ + (tagMetadata.tag
      -> floats.map(_.toDouble)))
  }

  private def readDoublesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val doubles = byteBuffer.getDoubleVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case ModelTransformationTag =>
        directory |-> modelTransformationLens set(Some(doubles))
      case DoublesTag => directory |-> doublesLens set(Some(doubles))
      case tag => directory |-> doublesMapLens modify(_ + (tag -> doubles))
    }
  }

}
