package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class Int16GeoTiffMultiBandTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  val cellType: CellType
) extends GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave)
    with Int16GeoTiffSegmentCollection {

  val noDataValue: Option[Short] = cellType match {
    case _: RawCellType => None
    case _: ConstantNoDataCellType => Some(shortNODATA)
    case ShortUserDefinedNoDataCellType(nd) => Some(nd)
  }

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner {
      private val arr = Array.ofDim[Short](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2s(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2s(v)
      }

      def getBytes(): Array[Byte] = {
        val result = new Array[Byte](targetSize * ShortConstantNoDataCellType.bytes)
        val bytebuff = ByteBuffer.wrap(result)
        bytebuff.asShortBuffer.put(arr)
        result
      }
    }
}
