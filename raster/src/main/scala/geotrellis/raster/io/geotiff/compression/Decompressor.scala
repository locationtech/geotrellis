package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader.{GeoTiffReaderLimitationException, MalformedGeoTiffException}
import java.nio.ByteOrder

trait Decompressor extends Serializable {
  def decompress(bytes: Array[Byte], sectionIndex: Int): Array[Byte]

  def flipEndian(bytesPerFlip: Int): Decompressor = 
    new Decompressor {
      def decompress(bytes: Array[Byte], sectionIndex: Int) =
        flip(Decompressor.this.decompress(bytes, sectionIndex))

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
}

object Decompressor {
  def apply(tags: Tags, byteOrder: ByteOrder): Decompressor = {
    import geotrellis.raster.io.geotiff.tags.codes.CompressionType._

    val d =
      tags.compression match {
        case Uncompressed => NoCompression
        case LZWCoded => LZWDecompressor(tags)
        case ZLibCoded | PkZipCoded => ZLibCompression.createDecompressor(tags)
        case PackBitsCoded => PackBitsDecompressor(tags)

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

    // If we need to flip the byte order, do at decompression time.
    if(byteOrder != ByteOrder.BIG_ENDIAN && tags.bitsPerPixel > 8) {
      d.flipEndian(tags.bytesPerPixel)
    } else {
      d
    }
  }
}
