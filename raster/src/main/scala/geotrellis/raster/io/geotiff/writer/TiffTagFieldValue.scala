package geotrellis.raster.io.geotiff.writer

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.codes._
import geotrellis.raster.io.geotiff.tags.codes.TagCodes._
import geotrellis.raster.io.geotiff.tags.codes.TiffFieldType._
import geotrellis.vector.Extent

import java.nio.ByteOrder
import scala.collection.mutable
import spire.syntax.cfor._

case class TiffTagFieldValue(
  tag: Int,
  fieldType: Int,
  length: Int,
  val value: Array[Byte]
) {
  assert(
    (fieldType match {
      case BytesFieldType => 1
      case AsciisFieldType => 1
      case ShortsFieldType => 2
      case IntsFieldType => 4
      case SignedBytesFieldType => 1
      case SignedShortsFieldType => 2
      case SignedIntsFieldType => 4
      case FloatsFieldType => 4
      case DoublesFieldType => 8
    }) * length == value.length, s"Unexpected tag value size for tag $tag.")
}

object TiffTagFieldValue {
  def apply(tag: Int, fieldType: Int, length: Int, value: Int)(implicit toBytes: ToBytes): TiffTagFieldValue =
    fieldType match {
      case ShortsFieldType => TiffTagFieldValue(tag, fieldType, length, toBytes(value.toShort))
      case IntsFieldType => TiffTagFieldValue(tag, fieldType, length, toBytes(value))
    }

  def createNoDataString(cellType: CellType): Option[String] =
    cellType match {
      case BitCellType | ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType | FloatCellType | DoubleCellType => None
      case ByteConstantNoDataCellType => Some(byteNODATA.toString)
      case ByteUserDefinedNoDataCellType(nd) => Some(nd.toString)
      case UByteConstantNoDataCellType => Some(ubyteNODATA.toString)
      case UByteUserDefinedNoDataCellType(nd) => Some(nd.toString)
      case ShortConstantNoDataCellType => Some(shortNODATA.toString)
      case ShortUserDefinedNoDataCellType(nd) => Some(nd.toString)
      case UShortConstantNoDataCellType => Some(ushortNODATA.toString)
      case UShortUserDefinedNoDataCellType(nd) => Some(nd.toString)
      case IntConstantNoDataCellType => Some(NODATA.toString)
      case IntUserDefinedNoDataCellType(nd) => Some(nd.toString)
      case FloatConstantNoDataCellType | DoubleConstantNoDataCellType => Some("nan")
      case FloatUserDefinedNoDataCellType(nd) => Some(nd.toDouble.toString) // Convert to a double, since there can be some weirdness with float toString.
      case DoubleUserDefinedNoDataCellType(nd) => Some(nd.toString)
    }

  def collect(geoTiff: GeoTiffData): (Array[TiffTagFieldValue], Array[Int] => TiffTagFieldValue) = {
    implicit val toBytes: ToBytes =
      if(geoTiff.imageData.decompressor.byteOrder == ByteOrder.BIG_ENDIAN)
        BigEndianToBytes
      else
        LittleEndianToBytes

    val imageData = geoTiff.imageData
    val extent = geoTiff.extent

    val fieldValues = mutable.ListBuffer[TiffTagFieldValue]()

    fieldValues += TiffTagFieldValue(ImageWidthTag, IntsFieldType, 1, imageData.cols)
    fieldValues += TiffTagFieldValue(ImageLengthTag, IntsFieldType, 1, imageData.rows)
    fieldValues += TiffTagFieldValue(BitsPerSampleTag, ShortsFieldType, 1, imageData.bandType.bitsPerSample)
    fieldValues += TiffTagFieldValue(CompressionTag, ShortsFieldType, 1, imageData.decompressor.code)
    fieldValues += TiffTagFieldValue(PhotometricInterpTag, ShortsFieldType, 1, 1.toShort)
    fieldValues += TiffTagFieldValue(SamplesPerPixelTag, ShortsFieldType, 1, imageData.bandCount)
    fieldValues += TiffTagFieldValue(PlanarConfigurationTag, ShortsFieldType, 1, PlanarConfigurations.PixelInterleave)
    fieldValues += TiffTagFieldValue(SampleFormatTag, ShortsFieldType, 1, imageData.bandType.sampleFormat)

    createNoDataString(geoTiff.cellType) match {
      case Some(noDataString) =>
        val bs = toBytes(noDataString)
        fieldValues += TiffTagFieldValue(GDALInternalNoDataTag, AsciisFieldType, bs.length, bs)
      case _ => ()
    }

    val re = RasterExtent(extent, imageData.cols, imageData.rows)
    fieldValues += TiffTagFieldValue(ModelPixelScaleTag, DoublesFieldType, 3, toBytes(Array(re.cellwidth, re.cellheight, 0.0)))

    fieldValues += TiffTagFieldValue(ModelTiePointsTag, DoublesFieldType, 6, toBytes(Array(0.0, 0.0, 0.0, extent.xmin, extent.ymax, 0.0)))

    // GeoKeyDirectory: Tags that describe the CRS
    val GeoDirectoryTags(shortGeoKeys, doubleGeoKeys) = CoordinateSystemParser.parse(geoTiff.crs, geoTiff.pixelSampleType)

    // Write the short values of the GeoKeyDirectory
    val shortValues = Array.ofDim[Short]( (shortGeoKeys.length + 1) * 4)
    shortValues(0) = 1
    shortValues(1) = 1
    shortValues(2) = 0
    shortValues(3) = shortGeoKeys.length.toShort
    cfor(0)(_ < shortGeoKeys.length, _ + 1) { i =>
      val start = (i + 1) * 4
      shortValues(start) = shortGeoKeys(i)._1.toShort
      shortValues(start + 1) = shortGeoKeys(i)._2.toShort
      shortValues(start + 2) = shortGeoKeys(i)._3.toShort
      shortValues(start + 3) = shortGeoKeys(i)._4.toShort
    }

    if(!shortValues.isEmpty) {
      fieldValues += TiffTagFieldValue(GeoKeyDirectoryTag, ShortsFieldType, shortValues.length, toBytes(shortValues))
    }

    if(!doubleGeoKeys.isEmpty) {
      fieldValues += TiffTagFieldValue(GeoDoubleParamsTag, DoublesFieldType, doubleGeoKeys.length, toBytes(doubleGeoKeys))
    }

    // Not written (what goes here?):
    //GeoKeyDirectory ASCII     TagCodes.GeoAsciiParamsTag, TiffFieldType.AsciisFieldType, N = Number of Characters (pipe sparated |), GeoKeyAsciis _

    // Metadata Tags
    // Account for special metadata tags, and then write out the rest in XML as the metadata tag
    val Tags(headerTags, bandTags) = geoTiff.tags
    var modifiedHeaderTags = headerTags
    geoTiff.pixelSampleType match {
      case Some(_) =>
        // This was captured in the GeoKeys already
        modifiedHeaderTags -= Tags.AREA_OR_POINT
      case None =>
    }

    modifiedHeaderTags.get(Tags.TIFFTAG_DATETIME) match {
      case Some(dateTime) =>
        val bs = toBytes(dateTime)
        fieldValues += TiffTagFieldValue(DateTimeTag, AsciisFieldType, bs.length, bs)
        modifiedHeaderTags -= Tags.TIFFTAG_DATETIME
      case None =>
    }

    val metadata = toBytes(new scala.xml.PrettyPrinter(80, 2).format(Tags(modifiedHeaderTags, geoTiff.tags.bandTags).toXml))
    fieldValues += TiffTagFieldValue(MetadataTag, AsciisFieldType, metadata.length, metadata)

    // Tags that are different if it is striped or tiled storage, and a function
    // that sets up a tag to point to the offsets of the image data.

    val segmentByteCounts = imageData.compressedBytes.map { _.length }.toArray
    val offsetsFieldValueBuilder: Array[Int] => TiffTagFieldValue =
      imageData.segmentLayout.storageMethod match {
        case Tiled(tileCols, tileRows) =>
          fieldValues += TiffTagFieldValue(TileWidthTag, IntsFieldType, 1, toBytes(tileCols))
          fieldValues += TiffTagFieldValue(TileLengthTag, IntsFieldType, 1, toBytes(tileRows))
          fieldValues += TiffTagFieldValue(TileByteCountsTag, IntsFieldType, segmentByteCounts.length, toBytes(segmentByteCounts))

          { offsets: Array[Int] => TiffTagFieldValue(TileOffsetsTag, IntsFieldType, offsets.length, toBytes(offsets)) }
        case s: Striped =>
          val rowsPerStrip = imageData.segmentLayout.tileLayout.tileRows
          fieldValues += {
            if(rowsPerStrip < Short.MaxValue) {
              TiffTagFieldValue(RowsPerStripTag, ShortsFieldType, 1, rowsPerStrip)
            } else {
              TiffTagFieldValue(RowsPerStripTag, IntsFieldType, 1, rowsPerStrip)
            }
          }
          fieldValues += TiffTagFieldValue(StripByteCountsTag, IntsFieldType, segmentByteCounts.length, toBytes(segmentByteCounts))

          { offsets: Array[Int] => TiffTagFieldValue(StripOffsetsTag, IntsFieldType, offsets.length, toBytes(offsets)) }
      }

    (fieldValues.toArray, offsetsFieldValueBuilder)
  }
}
