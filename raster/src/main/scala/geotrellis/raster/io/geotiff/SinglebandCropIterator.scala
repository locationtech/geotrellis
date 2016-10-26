package geotrellis.raster.io.geotiff

import geotrellis.raster._

class SinglebandCropIterator(val geoTiff: SinglebandGeoTiff,
  val windowedCols: Int,
  val windowedRows: Int) extends CropIterator {

  def adjustValues: Unit = {
    if (colCount + 1 == colIterator) {
      colCount = 0
      rowCount += 1
      overallColCount += 1
      overallRowCount += 1
    } else {
      colCount += 1
      overallColCount += 1
    }

    colMin = windowedCols * colCount
    rowMin = windowedRows * rowCount

    colMax =
      math.min(windowedCols + (windowedCols * colCount), wholeCols)
    rowMax =
      math.min(windowedRows + (windowedRows * rowCount), wholeRows)
  }
  
  def next: SinglebandGeoTiff = {
    if (colCount + 1 > colIterator)
      adjustValues

    val result: Raster[Tile] = geoTiff.crop(colMin, rowMin, colMax, rowMax)
    adjustValues
    SinglebandGeoTiff(result, result._2, geoTiff.crs)
  }
}
