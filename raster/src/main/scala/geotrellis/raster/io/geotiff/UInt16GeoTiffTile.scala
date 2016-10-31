package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt16GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: UShortCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with UInt16GeoTiffSegmentCollection {

  val noDataValue: Option[Int] = cellType match {
    case UShortCellType => None
    case UShortConstantNoDataCellType => Some(0)
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
          val data = segment.getRaw(i)
          arr(row * cols + col) = data
        }
      }
    }

    UShortArrayTile(arr, cols, rows, cellType)
  }

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size * UShortConstantNoDataCellType.bytes)
    var counter = 0

    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * UShortConstantNoDataCellType.bytes
          val adjEnd = intersection.end * UShortConstantNoDataCellType.bytes
          val adjCols = cols * UShortConstantNoDataCellType.bytes
          val adjWidth = result.width * UShortConstantNoDataCellType.bytes
          
          cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
            System.arraycopy(segment.bytes, i, arr, counter, adjWidth)
            counter += adjWidth
          }
        }
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) {i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)
          val segmentTransform = segmentLayout.getSegmentTransform(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * UShortConstantNoDataCellType.bytes
          val adjEnd = intersection.end * UShortConstantNoDataCellType.bytes
          val adjWidth = result.width * UShortConstantNoDataCellType.bytes
          val adjCols = intersection.tileWidth * UShortConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
            val col = segmentTransform.indexToCol(i / UShortConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / UShortConstantNoDataCellType.bytes)
            val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
            System.arraycopy(segment.bytes, i, arr, j * UShortConstantNoDataCellType.bytes, adjWidth)
          }
        }
      }
    }
    UShortArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }

  def withNoData(noDataValue: Option[Double]): UInt16GeoTiffTile =
    new UInt16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: UShortCells with NoDataHandling =>
        new UInt16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
