package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Int16GeoTiffTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val noDataValue: Option[Double]
) extends GeoTiffTile(segmentLayout, compression) with Int16GeoTiffSegmentCollection {
  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeShort.bytes)

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
        val width = segmentTransform.segmentCols * TypeShort.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeShort.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeShort.bytes)
          val row = segmentTransform.indexToRow(i / TypeShort.bytes)
          val j = ((row * cols) + col) * TypeShort.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    noDataValue match {
      case Some(nd) if isData(nd) && Short.MinValue.toDouble <= nd && nd <= Short.MaxValue.toDouble =>
        ShortArrayTile.fromBytes(arr, cols, rows, nd.toShort)
      case _ =>
        ShortArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
