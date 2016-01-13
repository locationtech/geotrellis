package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Int16GeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: CellType
) extends GeoTiffTile(segmentLayout, compression) with Int16GeoTiffSegmentCollection {

  val noDataValue = cellType match {
    case _: RawCellType => None
    case _: ConstantNoDataCellType => Some(shortNODATA)
    case ShortUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * ShortConstantNoDataCellType.bytes)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          getSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols * ShortConstantNoDataCellType.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * ShortConstantNoDataCellType.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / ShortConstantNoDataCellType.bytes)
          val row = segmentTransform.indexToRow(i / ShortConstantNoDataCellType.bytes)
          val j = ((row * cols) + col) * ShortConstantNoDataCellType.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    noDataValue match {
      case None => RawShortArrayTile.fromBytes(arr, cols, rows)
      case Some(nd) if (nd != Short.MinValue) => ???
      case Some(nd) => ShortArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
