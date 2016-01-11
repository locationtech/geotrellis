package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt16GeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  cellType: DynamicCellType
) extends GeoTiffTile(segmentLayout, compression, cellType) with UInt16GeoTiffSegmentCollection {

  val noDataValue = cellType.noDataValue

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Short](cols * rows)
    val ndShort = cellType.noDataValue
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = 
        getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          val data =
            if (segment.get(i) != ndShort) segment.getRaw(i)
            else shortNODATA
          arr(row * cols + col) = data
        }
      }
    }

    UShortArrayTile(arr, cols, rows)
  }
}

class RawUInt16GeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression
) extends GeoTiffTile(segmentLayout, compression, TypeRawUShort) with RawUInt16GeoTiffSegmentCollection {
  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Short](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment =
        getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          arr(row * cols + col) = segment.getRaw(i)
        }
      }
    }

    RawUShortArrayTile(arr, cols, rows)
  }
}

