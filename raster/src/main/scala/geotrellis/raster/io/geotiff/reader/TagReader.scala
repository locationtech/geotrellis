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

import geotrellis.raster.io.geotiff.reader.Tags._
import geotrellis.raster.io.geotiff.reader.TiffFieldType._

import java.nio.ByteBuffer

import monocle.syntax._

import spire.syntax.cfor._

import scala.collection._

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

    (directory &|->
      ImageDirectory._geoTiffTags ^|->
      GeoTiffTags._modelPixelScale set(Some(scaleX, scaleY, scaleZ)))
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

    (directory &|->
      ImageDirectory._geoTiffTags ^|->
      GeoTiffTags._modelTiePoints set(Some(points)))
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

    (directory &|->
      ImageDirectory._geoTiffTags ^|->
      GeoTiffTags._geoKeyDirectory set(Some(geoKeyDirectory)))
  }

  private def readBytesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val bytes = byteBuffer.getByteArray(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case DotRangeTag => directory &|->
        ImageDirectory._cmykTags ^|->
        CmykTags._dotRange set(Some(bytes.map(_.toInt)))
      case ExtraSamplesTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._extraSamples set(Some(bytes.map(_.toInt)))
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tag -> bytes.map(_.toLong)))
    }
  }

  private def readAsciisTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val string = byteBuffer.getString(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case ImageDescTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._imageDesc set(Some(string))
      case MakerTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._maker set(Some(string))
      case ModelTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._model set(Some(string))
      case SoftwareTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._software set(Some(string))
      case ArtistTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._artist set(Some(string))
      case HostComputerTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._hostComputer set(Some(string))
      case CopyrightTag => directory &|->
        ImageDirectory._metadataTags ^|->
        MetadataTags._copyright set(Some(string))
      case AsciisTag => directory &|->
        ImageDirectory._geoTiffTags ^|->
        GeoTiffTags._asciis set(Some(string))
      case MetadataTag => directory &|->
        ImageDirectory._geoTiffTags ^|->
        GeoTiffTags._metadata set(Some(string))
      case GDALInternalNoDataTag => directory.setGDALNoData(string)
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._asciisMap modify(_ + (tag -> string))
    }
  }

  private def readShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getShortArray(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case SubfileTypeTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._subfileType set(Some(shorts(0)))
      case ImageWidthTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._imageWidth set(shorts(0))
      case ImageLengthTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._imageLength set(shorts(0))
      case CompressionTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._compression set(shorts(0))
      case PhotometricInterpTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._photometricInterp set(shorts(0))
      case ThresholdingTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._thresholding set(shorts(0))
      case CellWidthTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._cellWidth set(Some(shorts(0)))
      case CellLengthTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._cellLength set(Some(shorts(0)))
      case FillOrderTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._fillOrder set((shorts(0)))
      case OrientationTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._orientation set(shorts(0))
      case SamplesPerPixelTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._samplesPerPixel set(shorts(0))
      case RowsPerStripTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._rowsPerStrip set(shorts(0))
      case PlanarConfigurationTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._planarConfiguration set(Some(shorts(0)))
      case GrayResponseUnitTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._grayResponseUnit set(Some(shorts(0)))
      case ResolutionUnitTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._resolutionUnit set(Some(shorts(0)))
      case PredictorTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._predictor set(Some(shorts(0)))
      case TileWidthTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileWidth set(Some(shorts(0)))
      case TileLengthTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileLength set(Some(shorts(0)))
      case InkSetTag => directory &|->
        ImageDirectory._cmykTags ^|->
        CmykTags._inkSet set(Some(shorts(0)))
      case NumberOfInksTag => directory &|->
        ImageDirectory._cmykTags ^|->
        CmykTags._numberOfInks set(Some(shorts(0)))
      case JpegProcTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegProc set(Some(shorts(0)))
      case JpegInterchangeFormatTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegInterchangeFormat set(Some(shorts(0)))
      case JpegInterchangeFormatLengthTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegInterchangeFormatLength set(Some(shorts(0)))
      case JpegRestartIntervalTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegRestartInterval set(Some(shorts(0)))
      case YCbCrPositioningTag => directory &|->
        ImageDirectory._yCbCrTags ^|->
        YCbCrTags._yCbCrPositioning set(Some(shorts(0)))
      case BitsPerSampleTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._bitsPerSample set(Some(shorts))
      case StripOffsetsTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._stripOffsets set(Some(shorts))
      case StripByteCountsTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._stripByteCounts set(Some(shorts))
      case MinSampleValueTag => directory &|->
        ImageDirectory._dataSampleFormatTags ^|->
        DataSampleFormatTags._minSampleValue set(Some(shorts.map(_.toLong)))
      case MaxSampleValueTag => directory &|->
        ImageDirectory._dataSampleFormatTags ^|->
        DataSampleFormatTags._maxSampleValue set(Some(shorts.map(_.toLong)))
      case GrayResponseCurveTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._grayResponseCurve set(Some(shorts))
      case PageNumberTag => directory &|->
        ImageDirectory._documentationTags ^|->
        DocumentationTags._pageNumber set(Some(shorts))
      case TransferFunctionTag => directory &|->
        ImageDirectory._colimetryTags ^|->
        ColimetryTags._transferFunction set(Some(shorts))
      case ColorMapTag => setColorMap(directory, shorts)
      case HalftoneHintsTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._halftoneHints set(Some(shorts))
      case TileByteCountsTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileByteCounts set(Some(shorts))
      case DotRangeTag => directory &|->
        ImageDirectory._cmykTags ^|->
        CmykTags._dotRange set(Some(shorts))
      case SampleFormatTag => directory &|->
        ImageDirectory._dataSampleFormatTags ^|->
        DataSampleFormatTags._sampleFormat set(shorts)
      case TransferRangeTag => directory &|->
        ImageDirectory._colimetryTags ^|->
        ColimetryTags._transferRange set(Some(shorts))
      case JpegLosslessPredictorsTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegLosslessPredictors set(Some(shorts))
      case JpegPointTransformsTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegPointTransforms set(Some(shorts))
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tag -> shorts.map(_.toLong)))
    }
  }

  private def setColorMap(directory: ImageDirectory, shorts: Array[Int]): ImageDirectory =
    if ((directory &|->
      ImageDirectory._basicTags ^|->
      BasicTags._photometricInterp get) == 3) {
      val divider = shorts.size / 3

      val arr = Array.ofDim[(Short, Short, Short)](divider)
      cfor(0)(_ < divider, _ + 1) { i =>
        arr(i) = (
          shorts(i).toShort,
          shorts(i + divider).toShort,
          shorts(i + 2 * divider).toShort
        )
      }

      (directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._colorMap set arr.toSeq)
    } else throw new MalformedGeoTiffException(
      "Colormap without Photometric Interpetation = 3."
    )

  private def readIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getIntArray(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case NewSubfileTypeTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._newSubfileType set(Some(ints(0)))
      case ImageWidthTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._imageWidth set(ints(0).toInt)
      case ImageLengthTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._imageLength set(ints(0).toInt)
      case T4OptionsTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._t4Options set(ints(0).toInt)
      case T6OptionsTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._t6Options set(Some(ints(0).toInt))
      case TileWidthTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileWidth set(Some(ints(0)))
      case TileLengthTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileLength set(Some(ints(0)))
      case JpegInterchangeFormatTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegInterchangeFormat set(Some(ints(0)))
      case JpegInterchangeFormatLengthTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegInterchangeFormatLength set(Some(ints(0)))
      case StripOffsetsTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._stripOffsets set(Some(ints.map(_.toInt)))
      case StripByteCountsTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._stripByteCounts set(Some(ints.map(_.toInt)))
      case FreeOffsetsTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._freeOffsets set(Some(ints))
      case FreeByteCountsTag => directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._freeByteCounts set(Some(ints))
      case TileOffsetsTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileOffsets set(Some(ints.map(_.toInt)))
      case TileByteCountsTag => directory &|->
        ImageDirectory._tileTags ^|->
        TileTags._tileByteCounts set(Some(ints.map(_.toInt)))
      case JpegQTablesTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegQTables set(Some(ints))
      case JpegDCTablesTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegDCTables set(Some(ints))
      case JpegACTablesTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegACTables set(Some(ints))
      case ReferenceBlackWhiteTag => directory &|->
        ImageDirectory._colimetryTags ^|->
        ColimetryTags._referenceBlackWhite set(Some(ints))
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tag -> ints.map(_.toLong)))
    }
  }

  private def readFractionalsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getFractionalArray(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case XResolutionTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._xResolution set(Some(fractionals(0)))
      case YResolutionTag => directory &|->
        ImageDirectory._basicTags ^|->
        BasicTags._yResolution set(Some(fractionals(0)))
      case XPositionTag => directory &|->
        ImageDirectory._documentationTags ^|->
        DocumentationTags._xPositions set(Some(fractionals))
      case YPositionTag => directory &|->
        ImageDirectory._documentationTags ^|->
        DocumentationTags._yPositions set(Some(fractionals))
      case WhitePointTag => directory &|->
        ImageDirectory._colimetryTags ^|->
        ColimetryTags._whitePoints set(Some(fractionals))
      case PrimaryChromaticitiesTag => directory &|->
        ImageDirectory._colimetryTags ^|->
        ColimetryTags._primaryChromaticities set(Some(fractionals))
      case YCbCrCoefficientsTag => directory &|->
        ImageDirectory._yCbCrTags ^|->
        YCbCrTags._yCbCrCoefficients set(Some(fractionals))
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._fractionalsMap modify(
          _ + (tag -> fractionals)
        )
    }
  }

  private def readSignedBytesTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val bytes = byteBuffer.getSignedByteArray(tagMetadata.length,
      tagMetadata.offset)

    (directory &|->
      ImageDirectory._nonStandardizedTags ^|->
      NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> bytes.map(_.toLong))))
  }

  private def readUndefinedTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val bytes = byteBuffer.getSignedByteArray(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case JpegTablesTag => directory &|->
        ImageDirectory._jpegTags ^|->
        JpegTags._jpegTables set(Some(bytes))
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._undefinedMap modify(_ + (tag -> bytes))
    }

  }

  private def readSignedShortsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getSignedShortArray(tagMetadata.length,
      tagMetadata.offset)

    (directory &|->
      ImageDirectory._nonStandardizedTags ^|->
      NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> shorts.map(_.toLong))))
  }

  private def readSignedIntsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getSignedIntArray(tagMetadata.length,
      tagMetadata.offset)

    (directory &|->
      ImageDirectory._nonStandardizedTags ^|->
      NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> ints.map(_.toLong))))
  }

  private def readSignedFractionalsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getSignedFractionalArray(tagMetadata.length,
      tagMetadata.offset)

    (directory &|->
      ImageDirectory._nonStandardizedTags ^|->
      NonStandardizedTags._fractionalsMap modify(
        _ + (tagMetadata.tag -> fractionals.map(x => (x._1.toLong, x._2.toLong)))
      ))
  }

  private def readFloatsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val floats = byteBuffer.getFloatArray(tagMetadata.length,
      tagMetadata.offset)

    (directory &|->
      ImageDirectory._nonStandardizedTags ^|->
      NonStandardizedTags._doublesMap modify(
        _ + (tagMetadata.tag -> floats.map(_.toDouble))
      ))
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

          (directory &|->
            ImageDirectory._geoTiffTags ^|->
            GeoTiffTags._modelTransformation set(Some(matrix)))
        }
      case DoublesTag => directory &|->
        ImageDirectory._geoTiffTags ^|->
        GeoTiffTags._doubles set(Some(doubles))
      case tag => directory &|->
        ImageDirectory._nonStandardizedTags ^|->
        NonStandardizedTags._doublesMap modify(_ + (tag -> doubles))
    }
  }

}
