package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class UByteGeoTiffMultiBandTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  cellType: DynamicCellType
) extends GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, cellType)
    with UByteGeoTiffSegmentCollection {

  val noDataValue = cellType.noDataValue

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner {
      private val arr = Array.ofDim[Byte](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2b(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2b(v)
      }

      def getBytes(): Array[Byte] = arr
    }
}

class RawUByteGeoTiffMultiBandTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean
) extends GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, TypeRawUByte)
    with RawUByteGeoTiffSegmentCollection {

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner {
      private val arr = Array.ofDim[Byte](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2b(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2b(v)
      }

      def getBytes(): Array[Byte] = arr
    }
}
