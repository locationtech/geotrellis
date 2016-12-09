package geotrellis.raster.io.geotiff

trait GridIndexTransform {
  def segmentIndex: Int
  def segmentLayout: GeoTiffSegmentLayout

  def layoutCols: Int = segmentLayout.tileLayout.layoutCols
  def layoutRows: Int = segmentLayout.tileLayout.layoutRows

  def tileCols: Int = segmentLayout.tileLayout.tileCols
  def tileRows: Int = segmentLayout.tileLayout.tileRows

  def layoutCol: Int = segmentIndex % layoutCols
  def layoutRow: Int = segmentIndex / layoutCols

  def segmentCols =
    if(layoutCol == layoutCols - 1)
      segmentLayout.totalCols - ( (layoutCols - 1) * tileCols)
    else
      tileCols

  def segmentRows =
    if(layoutRow == layoutRows - 1)
      segmentLayout.totalRows - ((layoutRows - 1) * tileRows)
    else
      tileRows

  /** The col of the source raster that this index represents. Can produce indefid cols */
  def indexToCol(i: Int) = {
    def tileCol = i % tileCols
    (layoutCol * tileCols) + tileCol
  }

  /** The row of the source raster that this index represents. Can produce indefid rows */
  def indexToRow(i: Int) = {
    def tileRow = i / tileCols
    (layoutRow * tileRows) + tileRow
  }

  /** Specific to BitGeoTiffSegment index. The col of the source raster that this index represents. */
  def bitIndexToCol(i: Int) = {
    def tileCol = i % segmentCols
    (layoutCol * tileCols) + tileCol
  }

  /** Specific to BitGeoTiffSegment index. The row of the source raster that this index represents. */
  def bitIndexToRow(i: Int) = {
    def tileRow = i / segmentCols
    (layoutRow * tileRows) + tileRow
  }

  def gridToIndex(col: Int, row: Int): Int
}
