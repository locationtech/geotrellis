package geotrellis.raster.io.geotiff

class StripedSegmentTransform(val segmentIndex: Int,
  val segmentLayout: GeoTiffSegmentLayout) extends GridIndexTransform {

  def gridToIndex(col: Int, row: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    tileRow * segmentCols + tileCol
  }
}

object StripedSegmentTransform {
  def apply(segmentId: Int, segmentLayout: GeoTiffSegmentLayout): StripedSegmentTransform =
    new StripedSegmentTransform(segmentId, segmentLayout)
}
