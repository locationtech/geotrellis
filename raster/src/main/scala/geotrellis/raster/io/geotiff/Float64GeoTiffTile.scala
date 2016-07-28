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
) extends GeoTiffTile(segmentLayout, compression) with CroppedGeoTiff with Float64GeoTiffSegmentCollection {

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

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    implicit val segLayout = segmentLayout
    implicit val gb = gridBounds
    
    val arr = Array.ofDim[Byte](gridBounds.size * DoubleConstantNoDataCellType.bytes)
    val adjWidth = width * DoubleConstantNoDataCellType.bytes
    
    val adjCols = cols * DoubleConstantNoDataCellType.bytes
    var counter = 0

    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        implicit val segmentId = i
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val adjStart = start * DoubleConstantNoDataCellType.bytes
          val adjEnd = end * DoubleConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
            System.arraycopy(segment.bytes, i, arr, counter, adjWidth)
            counter += adjWidth
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        implicit val segmentId = i
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)
          val adjTileWidth = tileWidth * DoubleConstantNoDataCellType.bytes

          val adjStart = start * DoubleConstantNoDataCellType.bytes
          val adjEnd = end * DoubleConstantNoDataCellType.bytes
          val adjDiff = diff * DoubleConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjTileWidth * segmentRows, _ + adjTileWidth) { i =>
            val col = segmentTransform.indexToCol(i / DoubleConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / DoubleConstantNoDataCellType.bytes)
            if (gridBounds.contains(col, row)) {
              val j = (row - rowMin) * width + (col - colMin)
              System.arraycopy(segment.bytes, i, arr, j * DoubleConstantNoDataCellType.bytes, adjDiff)
            }
          }
        }
      }
    }
    DoubleArrayTile.fromBytes(arr, width, height, cellType)
  }
}
