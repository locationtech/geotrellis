package geotrellis.raster.io.geotiff

trait SegmentTransform {
  def segmentIndex: Int
  def segmentLayout: GeoTiffSegmentLayout

  def layoutCols: Int = segmentLayout.tileLayout.layoutCols
  def layoutRows: Int = segmentLayout.tileLayout.layoutRows

  def tileCols: Int = segmentLayout.tileLayout.tileCols
  def tileRows: Int = segmentLayout.tileLayout.tileRows

  def layoutCol: Int = segmentIndex % layoutCols
  def layoutRow: Int = segmentIndex / layoutCols

  val (segmentCols, segmentRows) =
    segmentLayout.getSegmentDimensions(segmentIndex)

  // TODO: Get rid of all these non-abstract methods.

  /** The col of the source raster that this index represents. Can produce invalid cols */
  def indexToCol(i: Int) = {
    def tileCol = i % tileCols
    (layoutCol * tileCols) + tileCol
  }

  /** The row of the source raster that this index represents. Can produce invalid rows */
  def indexToRow(i: Int) = {
    def tileRow = i / tileCols
    (layoutRow * tileRows) + tileRow
  }

  def gridToIndex(col: Int, row: Int): Int
}


case class StripedSegmentTransform(segmentIndex: Int, segmentLayout: GeoTiffSegmentLayout) extends SegmentTransform {
  def gridToIndex(col: Int, row: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    tileRow * segmentCols + tileCol
  }
}

case class TiledSegmentTransform(segmentIndex: Int, segmentLayout: GeoTiffSegmentLayout) extends SegmentTransform {
  def gridToIndex(col: Int, row: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    tileRow * tileCols + tileCol
  }
}
