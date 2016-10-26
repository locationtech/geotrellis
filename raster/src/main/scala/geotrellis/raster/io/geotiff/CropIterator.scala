package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait CropIterator extends Iterator[GeoTiff[Tile]] {
  def geoTiff: GeoTiff[Tile]
  def windowedCols: Int
  def windowedRows: Int

  def wholeCols: Int = geoTiff.imageData.cols
  def wholeRows: Int = geoTiff.imageData.rows

  def colIterator: Int =
    if (wholeCols % windowedCols == 0)
      wholeCols / windowedCols
    else
      wholeCols / windowedCols + 1
  
  def rowIterator: Int =
    if (wholeRows % windowedRows == 0)
      wholeRows / windowedRows
    else
      wholeRows / windowedRows + 1

  var colCount = 0
  var rowCount = 0
  
  var colMin = 0
  var rowMin = 0

  var colMax = math.min(windowedCols, wholeCols)
  var rowMax = math.min(windowedRows, wholeRows)

  var overallColCount = 1
  var overallRowCount = 1

  def hasNext: Boolean =
    if (overallColCount <= colIterator || overallRowCount <= rowIterator) {
      true
    } else {
      false
    }

  def next: GeoTiff[Tile]
}
