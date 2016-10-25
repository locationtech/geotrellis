package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class Float64GeoTiffMultibandTile(
  compressedBytes: SegmentBytes,
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  val cellType: DoubleCells with NoDataHandling
) extends GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave)
    with Float64GeoTiffSegmentCollection {

  val noDataValue: Option[Double] = cellType match {
    case DoubleCellType => None
    case DoubleConstantNoDataCellType => Some(Double.NaN)
    case DoubleUserDefinedNoDataCellType(nd) => Some(nd)
  }

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner(bandCount) {
      private val arr = Array.ofDim[Double](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2d(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = v
      }

      def getBytes(): Array[Byte] = {
        val result = new Array[Byte](targetSize * DoubleConstantNoDataCellType.bytes)
        val bytebuff = ByteBuffer.wrap(result)
        bytebuff.asDoubleBuffer.put(arr)
        result
      }
    }

  def withNoData(noDataValue: Option[Double]): Float64GeoTiffMultibandTile =
    new Float64GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffMultibandTile  = {
    newCellType match {
      case dt: DoubleCells with NoDataHandling =>
        new Float64GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }

}

