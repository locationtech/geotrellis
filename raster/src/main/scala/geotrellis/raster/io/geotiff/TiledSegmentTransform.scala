package geotrellis.raster.io.geotiff

class TiledSegmentTransform(val segmentIndex: Int,
  val segmentLayout: GeoTiffSegmentLayout) extends GridIndexTransform {

  def gridToIndex(col: Int, row: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    tileRow * tileCols + tileCol
  }
}

object TiledSegmentTransform {
  def apply(segmentId: Int, segmentLayout: GeoTiffSegmentLayout): TiledSegmentTransform =
    new TiledSegmentTransform(segmentId, segmentLayout)
}
