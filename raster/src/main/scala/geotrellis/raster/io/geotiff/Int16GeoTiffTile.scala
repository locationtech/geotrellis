package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

import java.nio.{ByteBuffer, ByteOrder}
import spire.syntax.cfor._

class Int16GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: ShortCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Int16GeoTiffSegmentCollection {

  val noDataValue: Option[Short] = cellType match {
    case ShortCellType => None
    case ShortConstantNoDataCellType => Some(Short.MinValue)
    case ShortUserDefinedNoDataCellType(nd) => Some(nd)
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
          val data = segment.get(i)
          arr(row * cols + col) = data
        }
      }
    }

    ShortArrayTile(arr, cols, rows, cellType)
  }

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Byte](gridBounds.size * ShortConstantNoDataCellType.bytes)
    var counter = 0

    if (segmentLayout.isStriped) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val segmentGridBounds = segmentLayout.getGridBounds(i)
        if (gridBounds.intersects(segmentGridBounds)) {
          val segment = getSegment(i)

          val result = gridBounds.intersection(segmentGridBounds).get
          val intersection = Intersection(segmentGridBounds, result, segmentLayout)

          val adjStart = intersection.start * ShortConstantNoDataCellType.bytes
          val adjEnd = intersection.end * ShortConstantNoDataCellType.bytes
          val adjCols = cols * ShortConstantNoDataCellType.bytes
          val adjWidth = result.width * ShortConstantNoDataCellType.bytes
          
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

          val adjStart = intersection.start * ShortConstantNoDataCellType.bytes
          val adjEnd = intersection.end * ShortConstantNoDataCellType.bytes
          val adjWidth = result.width * ShortConstantNoDataCellType.bytes
          val adjTileWidth = intersection.tileWidth * ShortConstantNoDataCellType.bytes

          cfor(adjStart)(_ < adjEnd, _ + adjTileWidth) { i =>
            val col = segmentTransform.indexToCol(i / ShortConstantNoDataCellType.bytes)
            val row = segmentTransform.indexToRow(i / ShortConstantNoDataCellType.bytes)
            if (gridBounds.contains(col, row)) {
              val j = (row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)
              System.arraycopy(segment.bytes, i, arr, j * ShortConstantNoDataCellType.bytes, adjWidth)
            }
          }
        }
      }
    }
    ShortArrayTile.fromBytes(arr, gridBounds.width, gridBounds.height, cellType)
  }

  def withNoData(noDataValue: Option[Double]): Int16GeoTiffTile =
    new Int16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: ShortCells with NoDataHandling =>
        new Int16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
