package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader.{GeoTiffReaderLimitationException, MalformedGeoTiffException}
import java.nio.ByteOrder
import spire.syntax.cfor._

trait Decompressor extends Serializable {
  def code: Int

  def byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

  def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte]

  /** Internally, we use the ByteBuffer's default byte ordering (BigEndian).
    * If the decompressed bytes are LittleEndian, flip 'em.
    */
  def flipEndian(bytesPerFlip: Int): Decompressor = 
    new Decompressor {
      def code = Decompressor.this.code

      override
      def byteOrder = ByteOrder.LITTLE_ENDIAN // Since we have to flip, image data is in Little Endian

      def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
        flip(Decompressor.this.decompress(bytes, segmentIndex))

      def flip(bytes: Array[Byte]): Array[Byte] = {
        val arr = bytes.clone
        val size = arr.size

        var i = 0
        while (i < size) {
          var j = 0
          while (j < bytesPerFlip) {
            arr(i + j) = bytes(i + bytesPerFlip - 1 - j)
            j += 1
          }

          i += bytesPerFlip
        }

        arr
      }
    }

  def withPredictor(predictor: Predictor): Decompressor =
    new Decompressor {
      def code = Decompressor.this.code

      def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
        predictor(Decompressor.this.decompress(bytes, segmentIndex), segmentIndex)
    }
}

object Decompressor {
  def apply(tiffTags: TiffTags, byteOrder: ByteOrder): Decompressor = {
    import geotrellis.raster.io.geotiff.tags.codes.CompressionType._

    def checkEndian(d: Decompressor): Decompressor =
      if(byteOrder != ByteOrder.BIG_ENDIAN && tiffTags.bitsPerPixel > 8) {
        d.flipEndian(tiffTags.bytesPerPixel / tiffTags.bandCount)
      } else {
        d
      }

    def checkPredictor(d: Decompressor): Decompressor = {
      val predictor = Predictor(tiffTags)
      if(predictor.checkEndian)
        checkEndian(d).withPredictor(predictor)
      else
        d.withPredictor(predictor)
    }

    val segmentCount = tiffTags.segmentCount
    val segmentSizes = Array.ofDim[Int](segmentCount)
    val bandCount = tiffTags.bandCount
    if(!tiffTags.hasPixelInterleave || bandCount == 1) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        segmentSizes(i) = tiffTags.imageSegmentByteSize(i).toInt
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        segmentSizes(i) = tiffTags.imageSegmentByteSize(i).toInt * tiffTags.bandCount
      }
    }

    tiffTags.compression match {
      case Uncompressed => 
        checkEndian(NoCompression)
      case LZWCoded => 
        checkPredictor(LZWDecompressor(segmentSizes))
      case ZLibCoded | PkZipCoded => 
        checkPredictor(DeflateCompression.createDecompressor(segmentSizes))
      case PackBitsCoded => 
        checkEndian(PackBitsDecompressor(segmentSizes))

      // Unsupported compression types
      case JpegCoded =>
        val msg = "compression type JPEG is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case HuffmanCoded =>
        val msg = "compression type CCITTRLE is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case GroupThreeCoded =>
        val msg = s"compression type CCITTFAX3 is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case GroupFourCoded =>
        val msg = s"compression type CCITTFAX4 is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case JpegOldCoded =>
        val msg = "old jpeg (compression = 6) is deprecated."
        throw new MalformedGeoTiffException(msg)
      case compression =>
        val msg = s"compression type $compression is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
    }
  }
}
