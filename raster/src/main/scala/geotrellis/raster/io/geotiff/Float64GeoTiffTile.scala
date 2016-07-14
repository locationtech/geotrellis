package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Float64GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: DoubleCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Float64GeoTiffSegmentCollection {

  val noDataValue: Option[Double] = cellType match {
    case DoubleCellType => None
    case DoubleConstantNoDataCellType => Some(Double.NaN)
    case DoubleUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * DoubleConstantNoDataCellType.bytes)

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
        val width = segmentTransform.segmentCols * DoubleConstantNoDataCellType.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * DoubleConstantNoDataCellType.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / DoubleConstantNoDataCellType.bytes)
          val row = segmentTransform.indexToRow(i / DoubleConstantNoDataCellType.bytes)
          val j = ((row * cols) + col) * DoubleConstantNoDataCellType.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    DoubleArrayTile.fromBytes(arr, cols, rows, cellType)
  }

  def mutable(windowedGeoTiff: WindowedGeoTiff): MutableArrayTile = {
    val windowedGridBounds = windowedGeoTiff.windowedGridBounds
    val intersectingSegments = windowedGeoTiff.intersectingSegments
    val arr = Array.ofDim[Byte](windowedGridBounds.size * DoubleConstantNoDataCellType.bytes)
    var counter = 0

    val colMin = windowedGridBounds.colMin 
    val colMax = windowedGridBounds.colMax 
    val rowMin = windowedGridBounds.rowMin 
    val rowMax = windowedGridBounds.rowMax 
    val width = windowedGridBounds.width * DoubleConstantNoDataCellType.bytes
    val adjCols = cols * DoubleConstantNoDataCellType.bytes

    if (segmentLayout.isStriped) {
      for (segmentIndex <- intersectingSegments) {
        val segment = getSegment(segmentIndex)
        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val rowStart = segmentTransform.indexToRow(0) 
        val rowEnd = segmentTransform.segmentRows

        val start =
          if (rowStart < rowMin)
            (((rowMin - rowStart) * cols) + colMin) * DoubleConstantNoDataCellType.bytes
          else
            colMin * DoubleConstantNoDataCellType.bytes

        val end =
          if (rowEnd >= rowMax)
            ((rowMax * cols) + colMax) * DoubleConstantNoDataCellType.bytes
          else
            segment.size * DoubleConstantNoDataCellType.bytes

        cfor(start)(_ < end, _ + adjCols) { i =>
          System.arraycopy(segment.bytes, i, arr, counter, width)
          counter += width
        }
      }
    } else {
      for (segmentIndex <- intersectingSegments) {
        val segment = getSegment(segmentIndex)
        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val colStart = segmentTransform.indexToCol(0)
        val rowStart = segmentTransform.indexToRow(0)
        val colsInSegment = segmentTransform.segmentCols
        val rowsInSegment = segmentTransform.segmentRows
        val tileWidth = segmentLayout.tileLayout.tileCols * DoubleConstantNoDataCellType.bytes
        val colEnd = colStart + colsInSegment
        val rowEnd =
          if (segmentIndex == 0)
            rowsInSegment
          else
            rowStart + rowsInSegment

        val start = 
          if (colStart <= colMin && rowStart <= rowMin)
            ((rowMin - rowStart) * segmentLayout.tileLayout.tileCols) + colMin
          else if (colStart >= colMin && rowStart < rowMin)
            (rowMin - rowStart) * segmentLayout.tileLayout.tileCols
          else if (colStart <= colMin && rowStart >= rowMin)
            colMin
          else
            0
          
        val end =
          if (colStart <= colMin && colEnd <= colMax && rowEnd < rowMax)
            (rowsInSegment * segmentLayout.tileLayout.tileCols)
          else if (colStart <= colMin && colEnd <= colMax && rowEnd >= rowMax)
            (((rowMax - rowStart) * segmentLayout.tileLayout.tileCols) + colEnd)
          else if (colEnd >= colMax && rowEnd <= rowMax)
            (((rowEnd - rowStart) * segmentLayout.tileLayout.tileCols) - (colEnd - colMax)) + 1
          else
            (((rowMax - rowStart) *segmentLayout.tileLayout.tileCols) + colMax) + 1
          
        val diff =
          if (colStart <= colMin && colEnd <= colMax)
            colEnd - colMin
          else if (colStart >= colMin && colEnd <= colMax)
            segmentLayout.tileLayout.tileCols + 1
          else if (colStart >= colMin && colEnd >= colMax)
            (colMax - colStart) + 1
          else
            windowedGridBounds.width + 1

        cfor(start * DoubleConstantNoDataCellType.bytes)(_ < end * DoubleConstantNoDataCellType.bytes, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / DoubleConstantNoDataCellType.bytes)
          val row = segmentTransform.indexToRow(i / DoubleConstantNoDataCellType.bytes)
          if (windowedGridBounds.contains(col, row)) {
            val j = (row - rowMin) * windowedGridBounds.width + (col - colMin) 
            System.arraycopy(segment.bytes, i, arr, j * DoubleConstantNoDataCellType.bytes, diff * DoubleConstantNoDataCellType.bytes)
          }
        }
      }
    }
    DoubleArrayTile.fromBytes(arr, windowedGridBounds.width, windowedGridBounds.height, cellType)
  }
}
