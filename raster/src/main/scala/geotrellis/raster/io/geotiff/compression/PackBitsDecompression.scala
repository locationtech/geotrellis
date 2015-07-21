package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.tags.codes.CompressionType._

import monocle.syntax._
import spire.syntax.cfor._

object PackBitsDecompressor {
  def apply(segmentSizes: Array[Int]): PackBitsDecompressor =
    new PackBitsDecompressor(segmentSizes)
}

class PackBitsDecompressor(segmentSizes: Array[Int]) extends Decompressor {
  def code = PackBitsCoded

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val size = segmentSizes(segmentIndex)

    val rowArray = new Array[Byte](size)

    var j = 0
    var total = 0
    val len = segment.length
    while (total != size && j < len) {

      val headerByte = segment(j)
      j += 1

      if (0 <= headerByte) {
        // next (headerByte + 1) values in segment are literal values
        val limit = total + headerByte + 1
        while(total < limit) {
          rowArray(total) = segment(j)
          total += 1
          j += 1
        }
      } else if (-128 < headerByte && headerByte < 0) {
        // The next byte of data repeated (1 - headerByte) times
        val b = segment(j)
        j += 1

        val limit = total + (1 - headerByte)
        while(total < limit) {
          rowArray(total) = b
          total += 1
        }
      }
    }

    rowArray
  }
}
