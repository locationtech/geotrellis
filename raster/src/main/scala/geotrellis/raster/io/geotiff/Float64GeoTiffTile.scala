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

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size * DoubleConstantNoDataCellType.bytes)
    var counter = 0
    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * DoubleConstantNoDataCellType.bytes
          val adjEnd = intersection.end * DoubleConstantNoDataCellType.bytes
          val adjCols = intersection.cols * DoubleConstantNoDataCellType.bytes
          val adjWidth = result.width * DoubleConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
            System.arraycopy(segment.bytes, i, arr, counter, adjWidth)
            counter += adjWidth
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)
          val segmentTransform = segmentLayout.getSegmentTransform(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * DoubleConstantNoDataCellType.bytes
          val adjEnd = intersection.end * DoubleConstantNoDataCellType.bytes
          val adjTileWidth = intersection.tileWidth * DoubleConstantNoDataCellType.bytes
          val adjWidth = result.width * DoubleConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjTileWidth) { i =>
            val col = segmentTransform.indexToCol(i / DoubleConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / DoubleConstantNoDataCellType.bytes)
            val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
            System.arraycopy(segment.bytes, i, arr, j * DoubleConstantNoDataCellType.bytes, adjWidth)
          }
        }
      }
    }
    DoubleArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }

  def withNoData(noDataValue: Option[Double]): Float64GeoTiffTile =
    new Float64GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: DoubleCells with NoDataHandling =>
        new Float64GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
