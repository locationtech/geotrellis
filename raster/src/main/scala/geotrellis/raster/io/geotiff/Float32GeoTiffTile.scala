package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Float32GeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val noDataValue: Option[Double]
) extends GeoTiffTile(segmentLayout, compression) with Float32GeoTiffSegmentCollection {
  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeFloat.bytes)

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
        val width = segmentTransform.segmentCols * TypeFloat.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeFloat.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeFloat.bytes)
          val row = segmentTransform.indexToRow(i / TypeFloat.bytes)
          val j = ((row * cols) + col) * TypeFloat.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    noDataValue match {
      case Some(nd) if isData(nd) && Float.MinValue.toDouble <= nd && nd <= Float.MaxValue.toDouble =>
        FloatArrayTile.fromBytes(arr, cols, rows, nd.toFloat)
      case _ =>
        FloatArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
