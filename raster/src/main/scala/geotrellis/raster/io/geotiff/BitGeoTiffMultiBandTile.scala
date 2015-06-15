package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class BitGeoTiffMultiBandTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  noDataValue: Option[Double]
) extends GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
    with BitGeoTiffSegmentCollection {

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner {
      private val arr = Array.ofDim[Byte](targetSize + 7 / 8)

      def set(targetIndex: Int, v: Int): Unit = {
        BitArrayTile.update(arr, targetIndex, v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        BitArrayTile.updateDouble(arr, targetIndex, v)
      }

      def getBytes(): Array[Byte] =
        arr
    }
}

