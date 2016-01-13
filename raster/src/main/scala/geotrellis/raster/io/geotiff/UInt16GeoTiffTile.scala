package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt16GeoTiffTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: CellType
) extends GeoTiffTile(segmentLayout, compression) with UInt16GeoTiffSegmentCollection {

  val noDataValue = cellType match {
    case _: RawCellType => None
    case _: ConstantNoDataCellType => Some(0.toInt)
    case UShortUserDefinedNoDataCellType(nd) => Some(nd)
  }

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
          val data =
            if (segment.get(i) != noDataValue) segment.getRaw(i)
            else shortNODATA
          arr(row * cols + col) = data
        }
      }
    }

    UShortArrayTile(arr, cols, rows)
  }
}
