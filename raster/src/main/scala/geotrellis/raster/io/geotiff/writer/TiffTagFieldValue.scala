/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.writer

import geotrellis.raster._
import geotrellis.raster.render.IndexedColorMap
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.codes._
import geotrellis.raster.io.geotiff.tags.codes.TagCodes._
import geotrellis.raster.io.geotiff.tags.codes.TiffFieldType._
import spire.syntax.cfor._

import scala.collection.mutable
import java.nio.ByteOrder

case class TiffTagFieldValue(
  tag: Int,
  fieldType: Int,
  length: Int,
  value: Array[Byte]
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
      case BitCellType | ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType |
           FloatCellType | DoubleCellType => None
      case ByteConstantNoDataCellType => Some(byteNODATA.toString)
      case UByteConstantNoDataCellType => Some(ubyteNODATA.toString)
      case ShortConstantNoDataCellType => Some(shortNODATA.toString)
      case UShortConstantNoDataCellType => Some(ushortNODATA.toString)
      case IntConstantNoDataCellType => Some(NODATA.toString)
      case FloatConstantNoDataCellType | DoubleConstantNoDataCellType => Some("nan")
      case ct: ByteUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
      case ct: UByteUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
      case ct: ShortUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
      case ct: UShortUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
      case ct: IntUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
      case ct: FloatUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
      case ct: DoubleUserDefinedNoDataCellType => Some(ct.widenedNoData.toString)
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
    fieldValues += TiffTagFieldValue(PredictorTag, ShortsFieldType, 1, imageData.decompressor.predictorCode)
    fieldValues += TiffTagFieldValue(PhotometricInterpTag, ShortsFieldType, 1, geoTiff.options.colorSpace)
    fieldValues += TiffTagFieldValue(SamplesPerPixelTag, ShortsFieldType, 1, imageData.bandCount)
    fieldValues += TiffTagFieldValue(SampleFormatTag, ShortsFieldType, 1, imageData.bandType.sampleFormat)

    imageData.segmentLayout.interleaveMethod match {
      case PixelInterleave =>
        fieldValues += TiffTagFieldValue(PlanarConfigurationTag, ShortsFieldType, 1, PlanarConfigurations.PixelInterleave)
      case BandInterleave =>
        fieldValues += TiffTagFieldValue(PlanarConfigurationTag, ShortsFieldType, 1, PlanarConfigurations.BandInterleave)
    }

    geoTiff.options.subfileType.foreach { sft =>
      fieldValues += TiffTagFieldValue(NewSubfileTypeTag, IntsFieldType, 1, toBytes(sft.code))
    }

    if(geoTiff.options.colorSpace == ColorSpace.Palette) {

      val bitsPerSample = imageData.bandType.bitsPerSample

      if(bitsPerSample > 16 || geoTiff.cellType.isFloatingPoint) {
        throw new IncompatibleGeoTiffOptionsException(
          "'Palette' color space only supported for 8 or 16 bit integral cell types.")
      }

      val divider = 1 << bitsPerSample

      for(cmap ← geoTiff.options.colorMap) {
        val palette = IndexedColorMap.toTiffPalette(cmap)
        val size = math.min(palette.size, divider)
        // Indexed color palette is stored as three consecutive arrays containing
        // red, green, and blue values, in that order
        val flattenedPalette = Array.ofDim[Short](divider * 3)
        cfor(0)(_ < size, _ + 1) { i =>
          val c = palette(i)
          flattenedPalette(i) = c._1
          flattenedPalette(i + divider) = c._2
          flattenedPalette(i + divider * 2) = c._3
        }

        fieldValues += TiffTagFieldValue(ColorMapTag, ShortsFieldType, flattenedPalette.length, toBytes(flattenedPalette))
      }
    }

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

    val metadata = toBytes(new scala.xml.PrettyPrinter(Int.MaxValue, 2).format(Tags(modifiedHeaderTags, geoTiff.tags.bandTags).toXml))
    fieldValues += TiffTagFieldValue(MetadataTag, AsciisFieldType, metadata.length, metadata)

    // Tags that are different if it is striped or tiled storage, and a function
    // that sets up a tag to point to the offsets of the image data.

    val segmentByteCounts = {
      val len = imageData.segmentBytes.length
      val arr = Array.ofDim[Int](len)
      cfor(0)(_ < len, _ + 1) { i =>
        arr(i) = imageData.segmentBytes.getSegmentByteCount(i)
      }
      arr
    }

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
