package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt32GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: FloatCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with UInt32GeoTiffSegmentCollection {

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Float](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment =
        getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          arr(row * cols + col) = segment.get(i)
        }
      }
    }
    FloatArrayTile(arr, cols, rows, cellType)
  }

  def crop(gridBounds: GridBounds): MutableArrayTile = {
    val arr = Array.ofDim[Float](gridBounds.size)

    cfor(0)(_ < segmentCount, _ + 1) {i =>
      val segmentGridBounds = segmentLayout.getGridBounds(i)
      if (gridBounds.intersects(segmentGridBounds)) {
        val segment = getSegment(i)
        val segmentTransform = segmentLayout.getSegmentTransform(i)

        val result = gridBounds.intersection(segmentGridBounds).get
        val intersection = Intersection(segmentGridBounds, result, segmentLayout)
        val iterator =
          if (segmentLayout.isStriped)
            intersection.cols
          else
            intersection.tileWidth

        cfor(intersection.start)(_ < intersection.end, _ + iterator) { i =>
          cfor(0)(_ < result.width, _ + 1) { j =>
            val col = segmentTransform.indexToCol(i + j)
            val row = segmentTransform.indexToRow(i + j)
            arr((row - gridBounds.rowMin) * gridBounds.width + (col - gridBounds.colMin)) = segment.get(i + j)
          }
        }
      }
    }
    FloatArrayTile(arr, gridBounds.width, gridBounds.height, cellType)
  }

  def withNoData(noDataValue: Option[Double]): UInt32GeoTiffTile =
    new UInt32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): GeoTiffTile = {
    newCellType match {
      case dt: FloatCells with NoDataHandling =>
        new UInt32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}
