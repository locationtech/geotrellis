package geotrellis.raster.io.geotiff

private [geotiff] trait SegmentTransform {
  def segmentIndex: Int
  def segmentLayoutTransform: GeoTiffSegmentLayoutTransform
  protected def segmentLayout = segmentLayoutTransform.segmentLayout

  protected def bandCount = segmentLayoutTransform.bandCount

  protected def layoutCols: Int = segmentLayout.tileLayout.layoutCols
  protected def layoutRows: Int = segmentLayout.tileLayout.layoutRows

  protected def tileCols: Int = segmentLayout.tileLayout.tileCols
  protected def tileRows: Int = segmentLayout.tileLayout.tileRows

  protected def layoutCol: Int = segmentIndex % layoutCols
  protected def layoutRow: Int = segmentIndex / layoutCols

  val (segmentCols, segmentRows) =
    segmentLayoutTransform.getSegmentDimensions(segmentIndex)

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

  /** For single band or band interleave */
  def gridToIndex(col: Int, row: Int): Int

  /** For pixel interleave multiband */
  def gridToIndex(col: Int, row: Int, bandOffset: Int): Int
}


private [geotiff] case class StripedSegmentTransform(segmentIndex: Int, segmentLayoutTransform: GeoTiffSegmentLayoutTransform) extends SegmentTransform {
  def gridToIndex(col: Int, row: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    tileRow * segmentCols + tileCol
  }

  def gridToIndex(col: Int, row: Int, bandOffset: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    (tileRow * segmentCols * bandCount) + (tileCol * bandCount) + bandOffset
  }
}

private [geotiff] case class TiledSegmentTransform(segmentIndex: Int, segmentLayoutTransform: GeoTiffSegmentLayoutTransform) extends SegmentTransform {
  def gridToIndex(col: Int, row: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    tileRow * tileCols + tileCol
  }

  def gridToIndex(col: Int, row: Int, bandOffset: Int): Int = {
    val tileCol = col - (layoutCol * tileCols)
    val tileRow = row - (layoutRow * tileRows)
    (tileRow * tileCols * bandCount) + (tileCol * bandCount) + bandOffset
  }
}
