package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Int32GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: IntCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Int32GeoTiffSegmentCollection {

  val noDataValue: Option[Int] = cellType match {
    case IntCellType => None
    case IntConstantNoDataCellType => Some(Int.MinValue)
    case IntUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * IntConstantNoDataCellType.bytes)
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
        val width = segmentTransform.segmentCols * IntConstantNoDataCellType.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * IntConstantNoDataCellType.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / IntConstantNoDataCellType.bytes)
          val row = segmentTransform.indexToRow(i / IntConstantNoDataCellType.bytes)
          val j = ((row * cols) + col) * IntConstantNoDataCellType.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    IntArrayTile.fromBytes(arr, cols, rows, cellType)
  }

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size * IntConstantNoDataCellType.bytes)
    var counter = 0

    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * IntConstantNoDataCellType.bytes
          val adjEnd = intersection.end * IntConstantNoDataCellType.bytes
          val adjCols = intersection.cols * IntConstantNoDataCellType.bytes
          val adjWidth = result.width * IntConstantNoDataCellType.bytes

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

          val adjStart = intersection.start * IntConstantNoDataCellType.bytes
          val adjEnd = intersection.end * IntConstantNoDataCellType.bytes
          val adjTileWidth = intersection.tileWidth * IntConstantNoDataCellType.bytes
          val adjWidth= result.width * IntConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjTileWidth) { i =>
            val col = segmentTransform.indexToCol(i / IntConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / IntConstantNoDataCellType.bytes)
            val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
            System.arraycopy(segment.bytes, i, arr, j * IntConstantNoDataCellType.bytes, adjWidth)
          }
        }
      }
    }
    IntArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }

  def withNoData(noDataValue: Option[Double]): Int32GeoTiffTile =
    new Int32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: IntCells with NoDataHandling =>
        new Int32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
