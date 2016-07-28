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
) extends GeoTiffTile(segmentLayout, compression) with CroppedGeoTiff with UInt32GeoTiffSegmentCollection {

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
    implicit val gb = gridBounds
    implicit val segLayout = segmentLayout
    val arr = Array.ofDim[Float](gridBounds.size)

    cfor(0)(_ < segmentCount, _ + 1) {i =>
      implicit val segmentId = i
      if (gridBounds.intersects(segmentGridBounds)) {
        val segment = getSegment(i)

        cfor(0)(_ < segment.size, _ + 1) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          if (gridBounds.contains(col, row))
            arr((row - rowMin) * width + (col - colMin)) = segment.get(i)
        }
      }
    }
    FloatArrayTile(arr, width, height, cellType)
  }
}
