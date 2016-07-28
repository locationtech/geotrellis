package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class ByteGeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: ByteCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with CroppedGeoTiff with ByteGeoTiffSegmentCollection {

  val noDataValue: Option[Byte] = cellType match {
    case ByteCellType => None
    case ByteConstantNoDataCellType => Some(Byte.MinValue)
    case ByteUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows)

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
        val width = segmentTransform.segmentCols
        val tileWidth = segmentLayout.tileLayout.tileCols

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          val j = (row * cols) + col
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    ByteArrayTile.fromBytes(arr, cols, rows, cellType)
  }

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    implicit val gb = gridBounds
    implicit val segLayout = segmentLayout
    val arr = Array.ofDim[Byte](gridBounds.size)
    var counter = 0
    
    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        implicit val segmentId = i
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          cfor(start)(_ < end, _ + cols) { i =>
            System.arraycopy(segment.bytes, i, arr, counter, width)
            counter += width
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        implicit val segmentId = i
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          cfor(start)(_ < tileWidth * segmentRows, _ + tileWidth) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if (gridBounds.contains(col, row)) {
              val j = (row - rowMin) * width + (col - colMin)
              System.arraycopy(segment.bytes, i, arr, j, diff)
            }
          }
        }
      }
    }
    ByteArrayTile.fromBytes(arr, width, height, cellType)
  }
}
