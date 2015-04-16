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

package geotrellis.raster.io.geotiff.tags

import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.tags.codes._
import TagCodes._
import TiffFieldType._

import java.nio.ByteBuffer

import monocle.syntax._

import spire.syntax.cfor._

import scala.collection._

case class TagMetadata(
  tag: Int,
  fieldType: Int,
  length: Int,
  offset: Int
)

object TagReader {

  implicit class ByteBufferTagReaderWrapper(val byteBuffer: ByteBuffer) extends AnyVal {
    def readModelPixelScaleTag(tags: Tags,
      tagMetadata: TagMetadata) = {

      val oldPos = byteBuffer.position

      byteBuffer.position(tagMetadata.offset)

      val scaleX = byteBuffer.getDouble
      val scaleY = byteBuffer.getDouble
      val scaleZ = byteBuffer.getDouble

      byteBuffer.position(oldPos)

      (tags &|->
        Tags._geoTiffTags ^|->
        GeoTiffTags._modelPixelScale set(Some(scaleX, scaleY, scaleZ)))
    }

    def readModelTiePointsTag(tags: Tags,
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

      (tags &|->
        Tags._geoTiffTags ^|->
        GeoTiffTags._modelTiePoints set(Some(points)))
    }

    def readGeoKeyDirectoryTag(tags: Tags,
      tagMetadata: TagMetadata) = {

      val oldPos = byteBuffer.position

      byteBuffer.position(tagMetadata.offset)

      val version = byteBuffer.getShort
      val keyRevision = byteBuffer.getShort
      val minorRevision = byteBuffer.getShort
      val numberOfKeys = byteBuffer.getShort

      val keyDirectoryMetadata = GeoKeyDirectoryMetadata(version, keyRevision,
        minorRevision, numberOfKeys)

      val geoKeyDirectory = GeoKeyReader.read(byteBuffer,
        tags, GeoKeyDirectory(count = numberOfKeys))

      byteBuffer.position(oldPos)

      (tags &|->
        Tags._geoTiffTags ^|->
        GeoTiffTags._geoKeyDirectory set(Some(geoKeyDirectory)))
    }

    def readBytesTag(tags: Tags,
      tagMetadata: TagMetadata) = {

      val bytes = byteBuffer.getByteArray(tagMetadata.length, tagMetadata.offset)

      tagMetadata.tag match {
        case DotRangeTag => tags &|->
          Tags._cmykTags ^|->
          CmykTags._dotRange set(Some(bytes.map(_.toInt)))
        case ExtraSamplesTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._extraSamples set(Some(bytes.map(_.toInt)))
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._longsMap modify(_ + (tag -> bytes.map(_.toLong)))
      }
    }

    def readAsciisTag(tags: Tags,
      tagMetadata: TagMetadata): Tags = {

      val string = byteBuffer.getString(tagMetadata.length, tagMetadata.offset)
      tagMetadata.tag match {
        case ImageDescTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._imageDesc set(Some(string))
        case MakerTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._maker set(Some(string))
        case ModelTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._model set(Some(string))
        case SoftwareTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._software set(Some(string))
        case ArtistTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._artist set(Some(string))
        case HostComputerTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._hostComputer set(Some(string))
        case CopyrightTag => tags &|->
          Tags._metadataTags ^|->
          MetadataTags._copyright set(Some(string))
        case AsciisTag => tags &|->
          Tags._geoTiffTags ^|->
          GeoTiffTags._asciis set(Some(string))
        case MetadataTag => tags &|->
          Tags._geoTiffTags ^|->
          GeoTiffTags._metadata set(Some(string))
        case GDALInternalNoDataTag =>
          tags.setGDALNoData(string)
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._asciisMap modify(_ + (tag -> string))
      }
    }

    def readShortsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val shorts = byteBuffer.getShortArray(tagMetadata.length,
        tagMetadata.offset)

      tagMetadata.tag match {
        case SubfileTypeTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._subfileType set(Some(shorts(0)))
        case ImageWidthTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._imageWidth set(shorts(0))
        case ImageLengthTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._imageLength set(shorts(0))
        case CompressionTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._compression set(shorts(0))
        case PhotometricInterpTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._photometricInterp set(shorts(0))
        case ThresholdingTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._thresholding set(shorts(0))
        case CellWidthTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._cellWidth set(Some(shorts(0)))
        case CellLengthTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._cellLength set(Some(shorts(0)))
        case FillOrderTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._fillOrder set((shorts(0)))
        case OrientationTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._orientation set(shorts(0))
        case SamplesPerPixelTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._samplesPerPixel set(shorts(0))
        case RowsPerStripTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._rowsPerStrip set(shorts(0))
        case PlanarConfigurationTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._planarConfiguration set(Some(shorts(0)))
        case GrayResponseUnitTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._grayResponseUnit set(Some(shorts(0)))
        case ResolutionUnitTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._resolutionUnit set(Some(shorts(0)))
        case PredictorTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._predictor set(Some(shorts(0)))
        case TileWidthTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileWidth set(Some(shorts(0)))
        case TileLengthTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileHeight set(Some(shorts(0)))
        case InkSetTag => tags &|->
          Tags._cmykTags ^|->
          CmykTags._inkSet set(Some(shorts(0)))
        case NumberOfInksTag => tags &|->
          Tags._cmykTags ^|->
          CmykTags._numberOfInks set(Some(shorts(0)))
        case JpegProcTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegProc set(Some(shorts(0)))
        case JpegInterchangeFormatTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegInterchangeFormat set(Some(shorts(0)))
        case JpegInterchangeFormatLengthTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegInterchangeFormatLength set(Some(shorts(0)))
        case JpegRestartIntervalTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegRestartInterval set(Some(shorts(0)))
        case YCbCrPositioningTag => tags &|->
          Tags._yCbCrTags ^|->
          YCbCrTags._yCbCrPositioning set(Some(shorts(0)))
        case BitsPerSampleTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._bitsPerSample set(Some(shorts))
        case StripOffsetsTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._stripOffsets set(Some(shorts))
        case StripByteCountsTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._stripByteCounts set(Some(shorts))
        case MinSampleValueTag => tags &|->
          Tags._dataSampleFormatTags ^|->
          DataSampleFormatTags._minSampleValue set(Some(shorts.map(_.toLong)))
        case MaxSampleValueTag => tags &|->
          Tags._dataSampleFormatTags ^|->
          DataSampleFormatTags._maxSampleValue set(Some(shorts.map(_.toLong)))
        case GrayResponseCurveTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._grayResponseCurve set(Some(shorts))
        case PageNumberTag => tags &|->
          Tags._documentationTags ^|->
          DocumentationTags._pageNumber set(Some(shorts))
        case TransferFunctionTag => tags &|->
          Tags._colimetryTags ^|->
          ColimetryTags._transferFunction set(Some(shorts))
        case ColorMapTag => setColorMap(tags, shorts)
        case HalftoneHintsTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._halftoneHints set(Some(shorts))
        case TileByteCountsTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileByteCounts set(Some(shorts))
        case DotRangeTag => tags &|->
          Tags._cmykTags ^|->
          CmykTags._dotRange set(Some(shorts))
        case SampleFormatTag => tags &|->
          Tags._dataSampleFormatTags ^|->
          DataSampleFormatTags._sampleFormat set(shorts)
        case TransferRangeTag => tags &|->
          Tags._colimetryTags ^|->
          ColimetryTags._transferRange set(Some(shorts))
        case JpegLosslessPredictorsTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegLosslessPredictors set(Some(shorts))
        case JpegPointTransformsTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegPointTransforms set(Some(shorts))
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._longsMap modify(_ + (tag -> shorts.map(_.toLong)))
      }
    }

    def setColorMap(tags: Tags, shorts: Array[Int]): Tags =
      if ((tags &|->
        Tags._basicTags ^|->
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

        (tags &|->
          Tags._basicTags ^|->
          BasicTags._colorMap set arr.toSeq)
      } else throw new MalformedGeoTiffException(
        "Colormap without Photometric Interpetation = 3."
      )

    def readIntsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val ints = byteBuffer.getIntArray(tagMetadata.length, tagMetadata.offset)

      tagMetadata.tag match {
        case NewSubfileTypeTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._newSubfileType set(Some(ints(0)))
        case ImageWidthTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._imageWidth set(ints(0).toInt)
        case ImageLengthTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._imageLength set(ints(0).toInt)
        case T4OptionsTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._t4Options set(ints(0).toInt)
        case T6OptionsTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._t6Options set(Some(ints(0).toInt))
        case TileWidthTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileWidth set(Some(ints(0)))
        case TileLengthTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileHeight set(Some(ints(0)))
        case JpegInterchangeFormatTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegInterchangeFormat set(Some(ints(0)))
        case JpegInterchangeFormatLengthTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegInterchangeFormatLength set(Some(ints(0)))
        case StripOffsetsTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._stripOffsets set(Some(ints.map(_.toInt)))
        case StripByteCountsTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._stripByteCounts set(Some(ints.map(_.toInt)))
        case FreeOffsetsTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._freeOffsets set(Some(ints))
        case FreeByteCountsTag => tags &|->
          Tags._nonBasicTags ^|->
          NonBasicTags._freeByteCounts set(Some(ints))
        case TileOffsetsTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileOffsets set(Some(ints.map(_.toInt)))
        case TileByteCountsTag => tags &|->
          Tags._tileTags ^|->
          TileTags._tileByteCounts set(Some(ints.map(_.toInt)))
        case JpegQTablesTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegQTables set(Some(ints))
        case JpegDCTablesTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegDCTables set(Some(ints))
        case JpegACTablesTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegACTables set(Some(ints))
        case ReferenceBlackWhiteTag => tags &|->
          Tags._colimetryTags ^|->
          ColimetryTags._referenceBlackWhite set(Some(ints))
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._longsMap modify(_ + (tag -> ints.map(_.toLong)))
      }
    }

    def readFractionalsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val fractionals = byteBuffer.getFractionalArray(tagMetadata.length,
        tagMetadata.offset)

      tagMetadata.tag match {
        case XResolutionTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._xResolution set(Some(fractionals(0)))
        case YResolutionTag => tags &|->
          Tags._basicTags ^|->
          BasicTags._yResolution set(Some(fractionals(0)))
        case XPositionTag => tags &|->
          Tags._documentationTags ^|->
          DocumentationTags._xPositions set(Some(fractionals))
        case YPositionTag => tags &|->
          Tags._documentationTags ^|->
          DocumentationTags._yPositions set(Some(fractionals))
        case WhitePointTag => tags &|->
          Tags._colimetryTags ^|->
          ColimetryTags._whitePoints set(Some(fractionals))
        case PrimaryChromaticitiesTag => tags &|->
          Tags._colimetryTags ^|->
          ColimetryTags._primaryChromaticities set(Some(fractionals))
        case YCbCrCoefficientsTag => tags &|->
          Tags._yCbCrTags ^|->
          YCbCrTags._yCbCrCoefficients set(Some(fractionals))
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._fractionalsMap modify(
            _ + (tag -> fractionals)
          )
      }
    }

    def readSignedBytesTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val bytes = byteBuffer.getSignedByteArray(tagMetadata.length,
        tagMetadata.offset)

      (tags &|->
        Tags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> bytes.map(_.toLong))))
    }

    def readUndefinedTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val bytes = byteBuffer.getSignedByteArray(tagMetadata.length, tagMetadata.offset)

      tagMetadata.tag match {
        case JpegTablesTag => tags &|->
          Tags._jpegTags ^|->
          JpegTags._jpegTables set(Some(bytes))
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._undefinedMap modify(_ + (tag -> bytes))
      }

    }

    def readSignedShortsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val shorts = byteBuffer.getSignedShortArray(tagMetadata.length,
        tagMetadata.offset)

      (tags &|->
        Tags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> shorts.map(_.toLong))))
    }

    def readSignedIntsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val ints = byteBuffer.getSignedIntArray(tagMetadata.length,
        tagMetadata.offset)

      (tags &|->
        Tags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> ints.map(_.toLong))))
    }

    def readSignedFractionalsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val fractionals = byteBuffer.getSignedFractionalArray(tagMetadata.length,
        tagMetadata.offset)

      (tags &|->
        Tags._nonStandardizedTags ^|->
        NonStandardizedTags._fractionalsMap modify(
          _ + (tagMetadata.tag -> fractionals.map(x => (x._1.toLong, x._2.toLong)))
        ))
    }

    def readFloatsTag(tags: Tags,
      tagMetadata: TagMetadata) = {
      val floats = byteBuffer.getFloatArray(tagMetadata.length,
        tagMetadata.offset)

      (tags &|->
        Tags._nonStandardizedTags ^|->
        NonStandardizedTags._doublesMap modify(
          _ + (tagMetadata.tag -> floats.map(_.toDouble))
        ))
    }

    def readDoublesTag(tags: Tags,
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

            (tags &|->
              Tags._geoTiffTags ^|->
              GeoTiffTags._modelTransformation set(Some(matrix)))
          }
        case DoublesTag => tags &|->
          Tags._geoTiffTags ^|->
          GeoTiffTags._doubles set(Some(doubles))
        case tag => tags &|->
          Tags._nonStandardizedTags ^|->
          NonStandardizedTags._doublesMap modify(_ + (tag -> doubles))
      }
    }
  }


  def read(byteBuffer: ByteBuffer, tags: Tags, tagMetadata: TagMetadata): Tags =
    (tagMetadata.tag, tagMetadata.fieldType) match {
      case (ModelPixelScaleTag, _) =>
        byteBuffer.readModelPixelScaleTag(tags, tagMetadata)
      case (ModelTiePointsTag, _) =>
        byteBuffer.readModelTiePointsTag(tags, tagMetadata)
      case (GeoKeyDirectoryTag, _) =>
        byteBuffer.readGeoKeyDirectoryTag(tags, tagMetadata)
      case (_, BytesFieldType) =>
        byteBuffer.readBytesTag(tags, tagMetadata)
      case (_, AsciisFieldType) =>
        byteBuffer.readAsciisTag(tags, tagMetadata)
      case (_, ShortsFieldType) =>
        byteBuffer.readShortsTag(tags, tagMetadata)
      case (_, IntsFieldType) =>
        byteBuffer.readIntsTag(tags, tagMetadata)
      case (_, FractionalsFieldType) =>
        byteBuffer.readFractionalsTag(tags, tagMetadata)
      case (_, SignedBytesFieldType) =>
        byteBuffer.readSignedBytesTag(tags, tagMetadata)
      case (_, UndefinedFieldType) =>
        byteBuffer.readUndefinedTag(tags, tagMetadata)
      case (_, SignedShortsFieldType) =>
        byteBuffer.readSignedShortsTag(tags, tagMetadata)
      case (_, SignedIntsFieldType) =>
        byteBuffer.readSignedIntsTag(tags, tagMetadata)
      case (_, SignedFractionalsFieldType) =>
        byteBuffer.readSignedFractionalsTag(tags, tagMetadata)
      case (_, FloatsFieldType) =>
        byteBuffer.readFloatsTag(tags, tagMetadata)
      case (_, DoublesFieldType) =>
        byteBuffer.readDoublesTag(tags, tagMetadata)
    }
}
