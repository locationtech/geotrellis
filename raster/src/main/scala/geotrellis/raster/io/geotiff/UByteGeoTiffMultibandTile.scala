package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.ByteBuffer

class UByteGeoTiffMultibandTile(
  compressedBytes: SegmentBytes,
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  bandCount: Int,
  hasPixelInterleave: Boolean,
  val cellType: UByteCells with NoDataHandling
) extends GeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave)
    with UByteGeoTiffSegmentCollection {

  val noDataValue: Option[Int] = cellType match {
    case UByteCellType => None
    case UByteConstantNoDataCellType => Some(0)
    case UByteUserDefinedNoDataCellType(nd) => Some(nd)
  }

  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner =
    new SegmentCombiner(bandCount) {
      private val arr = Array.ofDim[Byte](targetSize)

      def set(targetIndex: Int, v: Int): Unit = {
        arr(targetIndex) = i2b(v)
      }

      def setDouble(targetIndex: Int, v: Double): Unit = {
        arr(targetIndex) = d2b(v)
      }

      def getBytes(): Array[Byte] = arr
    }

  def withNoData(noDataValue: Option[Double]): UByteGeoTiffMultibandTile =
    new UByteGeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffMultibandTile = {
    newCellType match {
      case dt: UByteCells with NoDataHandling =>
        new UByteGeoTiffMultibandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }

}
