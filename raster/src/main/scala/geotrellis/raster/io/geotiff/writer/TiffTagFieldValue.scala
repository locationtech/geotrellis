package geotrellis.raster.io.geotiff.writer

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.codes._
import geotrellis.raster.io.geotiff.tags.codes.TagCodes._
import geotrellis.raster.io.geotiff.tags.codes.TiffFieldType._
import geotrellis.vector.Extent

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
  def apply(tag: Int, fieldType: Int, length: Int, value: Int): TiffTagFieldValue = 
    fieldType match {
      case ShortsFieldType => TiffTagFieldValue(tag, fieldType, length, toBytes(value.toShort))
      case IntsFieldType => TiffTagFieldValue(tag, fieldType, length, toBytes(value))
    }

  def toBytes(i: Int): Array[Byte] = Array((i >>> 24).toByte, (i >>> 16).toByte, (i >>> 8).toByte, i.toByte)
  def toBytes(i: Short): Array[Byte] = Array((i >>> 8).toByte, i.toByte)
  def toBytes(i: Long): Array[Byte] =
    Array( 
      (i >>> 56).toByte,
      (i >>> 48).toByte,
      (i >>> 40).toByte,
      (i >>> 32).toByte,
      (i >>> 24).toByte, 
      (i >>> 16).toByte, 
      (i >>> 8).toByte, 
      i.toByte
    )

  def toBytes(f: Float): Array[Byte] = toBytes(java.lang.Float.floatToIntBits(f))
  def toBytes(d: Double): Array[Byte] = toBytes(java.lang.Double.doubleToLongBits(d))

  def toBytes(s: String): Array[Byte] = s.getBytes :+ 0x00.toByte

  def toBytes(arr: Array[Int]): Array[Byte] = {
    val result = Array.ofDim[Byte](arr.length * 4)
    var resultIndex = 0
    cfor(0)(_ < arr.length, _ + 1) { i =>
      val bytes = toBytes(arr(i))
      System.arraycopy(bytes, 0, result, resultIndex, bytes.length)
      resultIndex += bytes.length
    }
    result
  }

  def toBytes(arr: Array[Double]): Array[Byte] = {
    val result = Array.ofDim[Byte](arr.length * 8)
    var resultIndex = 0
    cfor(0)(_ < arr.length, _ + 1) { i =>
      val bytes = toBytes(arr(i))
      System.arraycopy(bytes, 0, result, resultIndex, bytes.length)
      resultIndex += bytes.length
    }
    result
  }

  def createNoDataString(cellType: CellType): Option[String] =
    cellType match {
      case TypeBit => None
      case TypeByte => Some(byteNODATA.toString)
      case TypeShort => Some(shortNODATA.toString)
      case TypeInt => Some(NODATA.toString)
      case (TypeFloat | TypeDouble) => Some("nan")
    }

  def collect(geoTiff: GeoTiff): (Array[TiffTagFieldValue], Array[Int] => TiffTagFieldValue) = {
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

    createNoDataString(imageData.bandType.cellType) match {
      case Some(noDataString) =>
        fieldValues += TiffTagFieldValue(GDALInternalNoDataTag, AsciisFieldType, noDataString.length + 1, toBytes(noDataString))
      case _ =>
    }

    val re = RasterExtent(extent, imageData.cols, imageData.rows)
    fieldValues += TiffTagFieldValue(ModelPixelScaleTag, DoublesFieldType, 3, toBytes(Array(re.cellwidth, re.cellheight, 0.0)))

    fieldValues += TiffTagFieldValue(ModelTiePointsTag, DoublesFieldType, 6, toBytes(Array(0.0, 0.0, 0.0, extent.xmin, extent.ymax, 0.0)))

    // GeoKeyDirectory

    // GeoDirectory shorts       TagCodes.GeoKeyDirectoryTag, TiffFieldType.ShortsFieldType, NumberOfKeys * 4 + 4, GeoKeyShorts _
        // GeoKeyShorts - (Short Short Short Short)
        //   first entry: (1, 1, 0, NumberOfKeys)
        //       entry 1: (s, s, s, s) first entry
        //       entry K: (s, s, s, s) entry NumberOfKeys

    // GeoKeyDirectory Doubles   TagCodes.GeoDoubleParamsTag, TiffFieldType.DoublesFieldType, N = Number of double values, GeoKeyDoubles _
    // GeoKeyDirectory ASCII     TagCodes.GeoAsciiParamsTag, TiffFieldType.AsciisFieldType, N = Number of Characters (pipe sparated |), GeoKeyAsciis _


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
          fieldValues += TiffTagFieldValue(RowsPerStripTag, IntsFieldType, 1, toBytes(rowsPerStrip))
          fieldValues += TiffTagFieldValue(StripByteCountsTag, IntsFieldType, segmentByteCounts.length, toBytes(segmentByteCounts))
          
          { offsets: Array[Int] => TiffTagFieldValue(StripOffsetsTag, IntsFieldType, offsets.length, toBytes(offsets)) }
      }

    (fieldValues.toArray, offsetsFieldValueBuilder)
  }
}
