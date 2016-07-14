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
) extends GeoTiffTile(segmentLayout, compression) with ByteGeoTiffSegmentCollection {

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

  def mutable(windowedGeoTiff: WindowedGeoTiff): MutableArrayTile = {
    val windowedGridBounds = windowedGeoTiff.windowedGridBounds
    val intersectingSegments = windowedGeoTiff.intersectingSegments
    val arr = Array.ofDim[Byte](windowedGridBounds.size)
    var counter = 0

    val colMin = windowedGridBounds.colMin
    val colMax = windowedGridBounds.colMax
    val rowMin = windowedGridBounds.rowMin
    val rowMax = windowedGridBounds.rowMax
    val width = windowedGridBounds.width

    if (segmentLayout.isStriped) {
      for (segmentIndex <- intersectingSegments) {
        val segment = segmentBytes.getSegment(segmentIndex)
        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val rowStart = segmentTransform.indexToRow(0)
        val rowEnd = segmentTransform.segmentRows
        val start =
          if (rowStart < rowMin) ((rowMin - rowStart) * cols) + colMin
          else colMin

        val end =
          if (rowEnd > rowMax) (rowMax * cols) + colMax
          else segment.size

        cfor(start)(_ < end, _ + cols) { i =>
          System.arraycopy(segment, i, arr, counter, width)
          counter += width
        }
      }
    } else {
      for (segmentIndex <- intersectingSegments) {
        val segment = getSegment(segmentIndex)
        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val colStart = segmentTransform.indexToCol(0)
        val rowStart = segmentTransform.indexToRow(0)
        val rowsInSegment = segmentTransform.segmentRows
        val tileWidth = segmentLayout.tileLayout.tileCols
        val colEnd = colStart + segmentTransform.segmentCols
        val rowEnd =
          if (segmentIndex == 0)
            rowsInSegment
          else
            rowStart + rowsInSegment

        val start = 
          if (colStart < colMin && rowStart < rowMin)
            ((rowMin - rowStart) * tileWidth) + colMin
          else if (colStart >= colMin && rowStart < rowMin)
            (rowMin - rowStart) * tileWidth
          else if (colStart <= colMin && rowStart > rowMin)
            colMin
          else
            0
          
        val end =
          if (colStart <= colMin && colEnd <= colMax && rowEnd < rowMax)
            (rowsInSegment * tileWidth)
          else if (colStart <= colMin && colEnd <= colMax && rowEnd >= rowMax)
            (((rowMax - rowStart) * tileWidth) + colEnd)
          else if (colEnd >= colMax && rowEnd <= rowMax)
            (((rowEnd - rowStart) * tileWidth) - (colEnd - colMax)) + 1
          else
            (((rowMax - rowStart) * tileWidth) + colMax) + 1
          
        val diff =
          if (colStart <= colMin && colEnd <= colMax)
            colEnd - colMin
          else if (colStart >= colMin && colEnd <= colMax)
            tileWidth + 1
          else if (colStart >= colMin && colEnd >= colMax)
            (colMax - colStart) + 1
          else
            width + 1

        cfor(start)(_ < end, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          if (windowedGridBounds.contains(col, row)) {
            val j = (row - rowMin) * width + (col - colMin)
            System.arraycopy(segment.bytes, i, arr, j, diff)
          }
        }
      }
    }
    ByteArrayTile.fromBytes(arr, windowedGridBounds.width, windowedGridBounds.height, cellType)
  }
}
