package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class Int32GeoTiffMultibandTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  val cellType: IntCells with NoDataHandling
) extends GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave)
    with Int32GeoTiffSegmentCollection {

  val noDataValue: Option[Int] = cellType match {
    case IntCellType => None
    case IntConstantNoDataCellType => Some(Int.MinValue)
    case IntUserDefinedNoDataCellType(nd) => Some(nd)
  }

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner(bandCount) {
      private val arr = Array.ofDim[Int](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = v
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2i(v)
      }

      def getBytes(): Array[Byte] = {
        val result = new Array[Byte](targetSize * IntConstantNoDataCellType.bytes)
        val bytebuff = ByteBuffer.wrap(result)
        bytebuff.asIntBuffer.put(arr)
        result
      }
    }
}
