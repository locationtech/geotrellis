package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class UInt16GeoTiffMultiBandTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  noDataValue: Option[Double]
) extends GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
    with UInt16GeoTiffSegmentCollection {

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner {
      private val arr = Array.ofDim[Int](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = v
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2i(v)
      }

      def getBytes(): Array[Byte] = {
        val result = new Array[Byte](targetSize * TypeInt.bytes)
        val bytebuff = ByteBuffer.wrap(result)
        bytebuff.asIntBuffer.put(arr)
        result
      }
    }
}
