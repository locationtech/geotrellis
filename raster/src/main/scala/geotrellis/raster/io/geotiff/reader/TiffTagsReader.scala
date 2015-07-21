package geotrellis.raster.io.geotiff.reader

import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.tags.codes._
import TagCodes._
import TiffFieldType._

import geotrellis.raster.io.geotiff.utils._
import spire.syntax.cfor._
import monocle.syntax._

import java.nio.{ ByteBuffer, ByteOrder }

object TiffTagsReader {
  def read(path: String): TiffTags = read(Filesystem.slurp(path))

  def read(bytes: Array[Byte]): TiffTags = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    // Set byteBuffer position
    byteBuffer.position(0)

    // set byte ordering
    (byteBuffer.get.toChar, byteBuffer.get.toChar) match {
      case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate GeoTiff identification number
    val geoTiffIdNumber = byteBuffer.getChar
    if ( geoTiffIdNumber != 42)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42, was $geoTiffIdNumber)")

    val tagsStartPosition = byteBuffer.getInt

    read(byteBuffer, tagsStartPosition)
  }

  def read(byteBuffer: ByteBuffer, tagsStartPosition: Int): TiffTags = {
    byteBuffer.position(tagsStartPosition)

    val tagCount = byteBuffer.getShort

    // Read the tags.
    var tiffTags = TiffTags()

    // Need to read geo tags last, relies on other tags already being read in.
    var geoTags: Option[TiffTagMetaData] = None

    cfor(0)(_ < tagCount, _ + 1) { i =>
      val tagMetaData =
        TiffTagMetaData(
          byteBuffer.getUnsignedShort, // Tag
          byteBuffer.getUnsignedShort, // Type
          byteBuffer.getInt,           // Count
          byteBuffer.getInt            // Offset
        )

      if (tagMetaData.tag == codes.TagCodes.GeoKeyDirectoryTag)
        geoTags = Some(tagMetaData)
      else 
        tiffTags = readTag(byteBuffer, tiffTags, tagMetaData)
    }

    geoTags match {
      case Some(t) => tiffTags = readTag(byteBuffer, tiffTags, t)
      case None =>
    }

    tiffTags
  }

  def readTag(byteBuffer: ByteBuffer, tiffTags: TiffTags, tagMetadata: TiffTagMetaData): TiffTags =
    (tagMetadata.tag, tagMetadata.fieldType) match {
      case (ModelPixelScaleTag, _) =>
        byteBuffer.readModelPixelScaleTag(tiffTags, tagMetadata)
      case (ModelTiePointsTag, _) =>
        byteBuffer.readModelTiePointsTag(tiffTags, tagMetadata)
      case (GeoKeyDirectoryTag, _) =>
        byteBuffer.readGeoKeyDirectoryTag(tiffTags, tagMetadata)
      case (_, BytesFieldType) =>
        byteBuffer.readBytesTag(tiffTags, tagMetadata)
      case (_, AsciisFieldType) =>
        byteBuffer.readAsciisTag(tiffTags, tagMetadata)
      case (_, ShortsFieldType) =>
        byteBuffer.readShortsTag(tiffTags, tagMetadata)
      case (_, IntsFieldType) =>
        byteBuffer.readIntsTag(tiffTags, tagMetadata)
      case (_, FractionalsFieldType) =>
        byteBuffer.readFractionalsTag(tiffTags, tagMetadata)
      case (_, SignedBytesFieldType) =>
        byteBuffer.readSignedBytesTag(tiffTags, tagMetadata)
      case (_, UndefinedFieldType) =>
        byteBuffer.readUndefinedTag(tiffTags, tagMetadata)
      case (_, SignedShortsFieldType) =>
        byteBuffer.readSignedShortsTag(tiffTags, tagMetadata)
      case (_, SignedIntsFieldType) =>
        byteBuffer.readSignedIntsTag(tiffTags, tagMetadata)
      case (_, SignedFractionalsFieldType) =>
        byteBuffer.readSignedFractionalsTag(tiffTags, tagMetadata)
      case (_, FloatsFieldType) =>
        byteBuffer.readFloatsTag(tiffTags, tagMetadata)
      case (_, DoublesFieldType) =>
        byteBuffer.readDoublesTag(tiffTags, tagMetadata)
    }

  implicit class ByteBufferTagReaderWrapper(val byteBuffer: ByteBuffer) extends AnyVal {
    def readModelPixelScaleTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {

      val oldPos = byteBuffer.position

      byteBuffer.position(tagMetadata.offset)

      val scaleX = byteBuffer.getDouble
      val scaleY = byteBuffer.getDouble
      val scaleZ = byteBuffer.getDouble

      byteBuffer.position(oldPos)

      (tiffTags &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._modelPixelScale set(Some(scaleX, scaleY, scaleZ)))
    }

    def readModelTiePointsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {

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

      (tiffTags &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._modelTiePoints set(Some(points)))
    }

    def readGeoKeyDirectoryTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {

      val oldPos = byteBuffer.position

      byteBuffer.position(tagMetadata.offset)

      val version = byteBuffer.getShort
      val keyRevision = byteBuffer.getShort
      val minorRevision = byteBuffer.getShort
      val numberOfKeys = byteBuffer.getShort

      val keyDirectoryMetadata = GeoKeyDirectoryMetadata(version, keyRevision,
        minorRevision, numberOfKeys)

      val geoKeyDirectory = GeoKeyReader.read(byteBuffer,
        tiffTags, GeoKeyDirectory(count = numberOfKeys))

      byteBuffer.position(oldPos)

      (tiffTags &|->
        TiffTags._geoTiffTags ^|->
        GeoTiffTags._geoKeyDirectory set(Some(geoKeyDirectory)))
    }

    def readBytesTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {

      val bytes = byteBuffer.getByteArray(tagMetadata.length, tagMetadata.offset)

      tagMetadata.tag match {
        case DotRangeTag => tiffTags &|->
          TiffTags._cmykTags ^|->
          CmykTags._dotRange set(Some(bytes.map(_.toInt)))
        case ExtraSamplesTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._extraSamples set(Some(bytes.map(_.toInt)))
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._longsMap modify(_ + (tag -> bytes.map(_.toLong)))
      }
    }

    def readAsciisTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData): TiffTags = {

      val string = byteBuffer.getString(tagMetadata.length, tagMetadata.offset)
      tagMetadata.tag match {
        case ImageDescTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._imageDesc set(Some(string))
        case MakerTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._maker set(Some(string))
        case ModelTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._model set(Some(string))
        case SoftwareTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._software set(Some(string))
        case ArtistTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._artist set(Some(string))
        case HostComputerTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._hostComputer set(Some(string))
        case CopyrightTag => tiffTags &|->
          TiffTags._metadataTags ^|->
          MetadataTags._copyright set(Some(string))
        case AsciisTag => tiffTags &|->
          TiffTags._geoTiffTags ^|->
          GeoTiffTags._asciis set(Some(string))
        case MetadataTag => tiffTags &|->
          TiffTags._geoTiffTags ^|->
          GeoTiffTags._metadata set(Some(string))
        case GDALInternalNoDataTag =>
          tiffTags.setGDALNoData(string)
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._asciisMap modify(_ + (tag -> string))
      }
    }

    def readShortsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val shorts = byteBuffer.getShortArray(tagMetadata.length,
        tagMetadata.offset)

      tagMetadata.tag match {
        case SubfileTypeTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._subfileType set(Some(shorts(0)))
        case ImageWidthTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._imageWidth set(shorts(0))
        case ImageLengthTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._imageLength set(shorts(0))
        case CompressionTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._compression set(shorts(0))
        case PhotometricInterpTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._photometricInterp set(shorts(0))
        case ThresholdingTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._thresholding set(shorts(0))
        case CellWidthTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._cellWidth set(Some(shorts(0)))
        case CellLengthTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._cellLength set(Some(shorts(0)))
        case FillOrderTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._fillOrder set((shorts(0)))
        case OrientationTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._orientation set(shorts(0))
        case SamplesPerPixelTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._samplesPerPixel set(shorts(0))
        case RowsPerStripTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._rowsPerStrip set(shorts(0))
        case PlanarConfigurationTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._planarConfiguration set(Some(shorts(0)))
        case GrayResponseUnitTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._grayResponseUnit set(Some(shorts(0)))
        case ResolutionUnitTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._resolutionUnit set(Some(shorts(0)))
        case PredictorTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._predictor set(Some(shorts(0)))
        case TileWidthTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileWidth set(Some(shorts(0)))
        case TileLengthTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileLength set(Some(shorts(0)))
        case InkSetTag => tiffTags &|->
          TiffTags._cmykTags ^|->
          CmykTags._inkSet set(Some(shorts(0)))
        case NumberOfInksTag => tiffTags &|->
          TiffTags._cmykTags ^|->
          CmykTags._numberOfInks set(Some(shorts(0)))
        case JpegProcTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegProc set(Some(shorts(0)))
        case JpegInterchangeFormatTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegInterchangeFormat set(Some(shorts(0)))
        case JpegInterchangeFormatLengthTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegInterchangeFormatLength set(Some(shorts(0)))
        case JpegRestartIntervalTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegRestartInterval set(Some(shorts(0)))
        case YCbCrPositioningTag => tiffTags &|->
          TiffTags._yCbCrTags ^|->
          YCbCrTags._yCbCrPositioning set(Some(shorts(0)))
        case BitsPerSampleTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._bitsPerSample set(shorts(0))
        case StripOffsetsTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._stripOffsets set(Some(shorts))
        case StripByteCountsTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._stripByteCounts set(Some(shorts))
        case MinSampleValueTag => tiffTags &|->
          TiffTags._dataSampleFormatTags ^|->
          DataSampleFormatTags._minSampleValue set(Some(shorts.map(_.toLong)))
        case MaxSampleValueTag => tiffTags &|->
          TiffTags._dataSampleFormatTags ^|->
          DataSampleFormatTags._maxSampleValue set(Some(shorts.map(_.toLong)))
        case GrayResponseCurveTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._grayResponseCurve set(Some(shorts))
        case PageNumberTag => tiffTags &|->
          TiffTags._documentationTags ^|->
          DocumentationTags._pageNumber set(Some(shorts))
        case TransferFunctionTag => tiffTags &|->
          TiffTags._colimetryTags ^|->
          ColimetryTags._transferFunction set(Some(shorts))
        case ColorMapTag => setColorMap(tiffTags, shorts)
        case HalftoneHintsTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._halftoneHints set(Some(shorts))
        case TileByteCountsTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileByteCounts set(Some(shorts))
        case DotRangeTag => tiffTags &|->
          TiffTags._cmykTags ^|->
          CmykTags._dotRange set(Some(shorts))
        case SampleFormatTag => tiffTags &|->
          TiffTags._dataSampleFormatTags ^|->
          DataSampleFormatTags._sampleFormat set(shorts(0))
        case TransferRangeTag => tiffTags &|->
          TiffTags._colimetryTags ^|->
          ColimetryTags._transferRange set(Some(shorts))
        case JpegLosslessPredictorsTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegLosslessPredictors set(Some(shorts))
        case JpegPointTransformsTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegPointTransforms set(Some(shorts))
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._longsMap modify(_ + (tag -> shorts.map(_.toLong)))
      }
    }

    def setColorMap(tiffTags: TiffTags, shorts: Array[Int]): TiffTags =
      if ((tiffTags &|->
        TiffTags._basicTags ^|->
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

        (tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._colorMap set arr.toSeq)
      } else throw new MalformedGeoTiffException(
        "Colormap without Photometric Interpetation = 3."
      )

    def readIntsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val ints = byteBuffer.getIntArray(tagMetadata.length, tagMetadata.offset)

      tagMetadata.tag match {
        case NewSubfileTypeTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._newSubfileType set(Some(ints(0)))
        case ImageWidthTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._imageWidth set(ints(0).toInt)
        case ImageLengthTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._imageLength set(ints(0).toInt)
        case T4OptionsTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._t4Options set(ints(0).toInt)
        case T6OptionsTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._t6Options set(Some(ints(0).toInt))
        case TileWidthTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileWidth set(Some(ints(0)))
        case TileLengthTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileLength set(Some(ints(0)))
        case JpegInterchangeFormatTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegInterchangeFormat set(Some(ints(0)))
        case JpegInterchangeFormatLengthTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegInterchangeFormatLength set(Some(ints(0)))
        case StripOffsetsTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._stripOffsets set(Some(ints.map(_.toInt)))
        case StripByteCountsTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._stripByteCounts set(Some(ints.map(_.toInt)))
        case FreeOffsetsTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._freeOffsets set(Some(ints))
        case FreeByteCountsTag => tiffTags &|->
          TiffTags._nonBasicTags ^|->
          NonBasicTags._freeByteCounts set(Some(ints))
        case TileOffsetsTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileOffsets set(Some(ints.map(_.toInt)))
        case TileByteCountsTag => tiffTags &|->
          TiffTags._tileTags ^|->
          TileTags._tileByteCounts set(Some(ints.map(_.toInt)))
        case JpegQTablesTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegQTables set(Some(ints))
        case JpegDCTablesTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegDCTables set(Some(ints))
        case JpegACTablesTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegACTables set(Some(ints))
        case ReferenceBlackWhiteTag => tiffTags &|->
          TiffTags._colimetryTags ^|->
          ColimetryTags._referenceBlackWhite set(Some(ints))
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._longsMap modify(_ + (tag -> ints.map(_.toLong)))
      }
    }

    def readFractionalsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val fractionals = byteBuffer.getFractionalArray(tagMetadata.length,
        tagMetadata.offset)

      tagMetadata.tag match {
        case XResolutionTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._xResolution set(Some(fractionals(0)))
        case YResolutionTag => tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._yResolution set(Some(fractionals(0)))
        case XPositionTag => tiffTags &|->
          TiffTags._documentationTags ^|->
          DocumentationTags._xPositions set(Some(fractionals))
        case YPositionTag => tiffTags &|->
          TiffTags._documentationTags ^|->
          DocumentationTags._yPositions set(Some(fractionals))
        case WhitePointTag => tiffTags &|->
          TiffTags._colimetryTags ^|->
          ColimetryTags._whitePoints set(Some(fractionals))
        case PrimaryChromaticitiesTag => tiffTags &|->
          TiffTags._colimetryTags ^|->
          ColimetryTags._primaryChromaticities set(Some(fractionals))
        case YCbCrCoefficientsTag => tiffTags &|->
          TiffTags._yCbCrTags ^|->
          YCbCrTags._yCbCrCoefficients set(Some(fractionals))
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._fractionalsMap modify(
            _ + (tag -> fractionals)
          )
      }
    }

    def readSignedBytesTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val bytes = byteBuffer.getSignedByteArray(tagMetadata.length,
        tagMetadata.offset)

      (tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> bytes.map(_.toLong))))
    }

    def readUndefinedTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val bytes = byteBuffer.getSignedByteArray(tagMetadata.length, tagMetadata.offset)

      tagMetadata.tag match {
        case JpegTablesTag => tiffTags &|->
          TiffTags._jpegTags ^|->
          JpegTags._jpegTables set(Some(bytes))
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._undefinedMap modify(_ + (tag -> bytes))
      }

    }

    def readSignedShortsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val shorts = byteBuffer.getSignedShortArray(tagMetadata.length,
        tagMetadata.offset)

      (tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> shorts.map(_.toLong))))
    }

    def readSignedIntsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val ints = byteBuffer.getSignedIntArray(tagMetadata.length,
        tagMetadata.offset)

      (tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._longsMap modify(_ + (tagMetadata.tag -> ints.map(_.toLong))))
    }

    def readSignedFractionalsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val fractionals = byteBuffer.getSignedFractionalArray(tagMetadata.length,
        tagMetadata.offset)

      (tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._fractionalsMap modify(
          _ + (tagMetadata.tag -> fractionals.map(x => (x._1.toLong, x._2.toLong)))
        ))
    }

    def readFloatsTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
      val floats = byteBuffer.getFloatArray(tagMetadata.length,
        tagMetadata.offset)

      (tiffTags &|->
        TiffTags._nonStandardizedTags ^|->
        NonStandardizedTags._doublesMap modify(
          _ + (tagMetadata.tag -> floats.map(_.toDouble))
        ))
    }

    def readDoublesTag(tiffTags: TiffTags,
      tagMetadata: TiffTagMetaData) = {
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

            (tiffTags &|->
              TiffTags._geoTiffTags ^|->
              GeoTiffTags._modelTransformation set(Some(matrix)))
          }
        case DoublesTag => tiffTags &|->
          TiffTags._geoTiffTags ^|->
          GeoTiffTags._doubles set(Some(doubles))
        case tag => tiffTags &|->
          TiffTags._nonStandardizedTags ^|->
          NonStandardizedTags._doublesMap modify(_ + (tag -> doubles))
      }
    }
  }

}
