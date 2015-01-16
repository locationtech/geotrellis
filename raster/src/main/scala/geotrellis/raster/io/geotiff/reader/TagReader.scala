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

import java.nio.ByteBuffer

import monocle.syntax._
import spire.syntax.cfor._

import scala.collection._

import geotrellis.raster.io.geotiff.reader.utils.ByteBufferUtils._

import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._

import geotrellis.raster.io.geotiff.reader.Tags._
import geotrellis.raster.io.geotiff.reader.TiffFieldType._

case class TagReader(byteBuffer: ByteBuffer) {

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

    val points = Array.ofDim[(Pixel3D, Pixel3D)](numberOfPoints)
    cfor(0)(_ < numberOfPoints, _ + 1) { i =>
      points(i) =
        (
          Pixel3D(
            byteBuffer.getDouble,
            byteBuffer.getDouble,
            byteBuffer.getDouble
          ),
          Pixel3D(
            byteBuffer.getDouble,
            byteBuffer.getDouble,
            byteBuffer.getDouble
          )
        )
    }

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

    val bytes = byteBuffer.getByteArray(tagMetadata.length, tagMetadata.offset)

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
      case MetadataTag => directory |-> metadataLens set(Some(string))
      case GDALInternalNoDataTag => directory.setGDALNoData(string)
      case tag => directory |-> asciisMapLens modify(_ + (tag -> string))
    }
  }

  private def readShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getShortArray(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case SubfileTypeTag => directory |-> subfileTypeLens set(Some(shorts(0)))
      case ImageWidthTag => directory |-> imageWidthLens set(shorts(0))
      case ImageLengthTag => directory |-> imageLengthLens set(shorts(0))
      case CompressionTag => directory |-> compressionLens set(shorts(0))
      case PhotometricInterpTag => directory |-> photometricInterpLens set(shorts(0))
      case ThresholdingTag => directory |-> thresholdingLens set(shorts(0))
      case CellWidthTag => directory |-> cellWidthLens set(Some(shorts(0)))
      case CellLengthTag => directory |-> cellLengthLens set(Some(shorts(0)))
      case FillOrderTag => directory |-> fillOrderLens set(shorts(0))
      case OrientationTag => directory |-> orientationLens set(shorts(0))
      case SamplesPerPixelTag => directory |-> samplesPerPixelLens set(shorts(0))
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
      case ColorMapTag => setColorMap(directory, shorts)
      case HalftoneHintsTag => directory |-> halftoneHintsLens set(Some(shorts))
      case TileByteCountsTag => directory |-> tileByteCountsLens set(
        Some(shorts))
      case DotRangeTag => directory |-> dotRangeLens set(Some(shorts))
      case SampleFormatTag =>
        directory |-> sampleFormatLens set(shorts)
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

  private def setColorMap(directory: ImageDirectory, shorts: Array[Int]): ImageDirectory =
    if ((directory |-> photometricInterpLens get) == 3) {
      val divider = shorts.size / 3

      var i = 0
      val arr = Array.ofDim[(Short, Short, Short)](divider)

      for (i <- 0 until divider)
        arr(i) = (shorts(i).toShort, shorts(i + divider).toShort,
          shorts(i + 2 * divider).toShort)

      directory |-> colorMapLens set Some(arr)
    }
    else throw new MalformedGeoTiffException(
      "Colormap without Photometric Interpetation = 3."
    )

  private def readIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getIntArray(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case NewSubfileTypeTag =>
        directory |-> newSubfileTypeLens set(Some(ints(0)))
      case ImageWidthTag => directory |-> imageWidthLens set(ints(0).toInt)
      case ImageLengthTag => directory |-> imageLengthLens set(ints(0).toInt)
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
    val fractionals = byteBuffer.getFractionalArray(tagMetadata.length,
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
    val bytes = byteBuffer.getSignedByteArray(tagMetadata.length,
      tagMetadata.offset)

    directory |-> longsMapLens modify (_ + (tagMetadata.tag
      -> bytes.map(_.toLong)))
  }

  private def readUndefinedTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val bytes = byteBuffer.getSignedByteArray(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case JpegTablesTag => directory |-> jpegTablesLens set(Some(bytes))
      case tag => directory |-> undefinedMapLens modify (_ + (tag -> bytes))
    }

  }

  private def readSignedShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getSignedShortArray(tagMetadata.length,
      tagMetadata.offset)

    directory |-> longsMapLens modify(_ + (tagMetadata.tag
      -> shorts.map(_.toLong)))
  }

  private def readSignedIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getSignedIntArray(tagMetadata.length,
      tagMetadata.offset)

    directory |-> longsMapLens modify(_ + (tagMetadata.tag ->
      ints.map(_.toLong)))
  }

  private def readSignedFractionalsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getSignedFractionalArray(tagMetadata.length,
      tagMetadata.offset)

    directory |-> fractionalsMapLens modify(_ + (tagMetadata.tag
      -> fractionals.map(x => (x._1.toLong, x._2.toLong))))
  }

  private def readFloatsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val floats = byteBuffer.getFloatArray(tagMetadata.length,
      tagMetadata.offset)

    directory |-> doublesMapLens modify(_ + (tagMetadata.tag
      -> floats.map(_.toDouble)))
  }

  private def readDoublesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val doubles = byteBuffer.getDoubleArray(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case ModelTransformationTag =>
        if (doubles.size != 16)
          throw new MalformedGeoTiffException("bad model tranformations")
        else {
          val matrix = Array(
            Array(doubles(0), doubles(1), doubles(2), doubles(3)),
            Array(doubles(4), doubles(5), doubles(6), doubles(7)),
            Array(doubles(8), doubles(9), doubles(10), doubles(11)),
            Array(doubles(12), doubles(13), doubles(14), doubles(15))
          )

          directory |-> modelTransformationLens set(Some(matrix))
        }
      case DoublesTag => directory |-> doublesLens set(Some(doubles))
      case tag => directory |-> doublesMapLens modify(_ + (tag -> doubles))
    }
  }

}
