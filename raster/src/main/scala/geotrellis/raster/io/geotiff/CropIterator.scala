/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff

import geotrellis.raster._

/**
 * This class is an extension of [[Iterator]] where it takes a GeoTif and the
 * size of the sub tiles which the file should be broken up into. The returned
 * values are these sub tiles.
 *
 * @param geoTiff: [[GeoTiff]] of type T <: [[CellGrid]]
 * @param widnowedCols: An Int that is max col size of the sub-tiles.
 * @param widnowedRows: An Int that is max row size of the sub-tiles.
 * @return: An [[Iterator]] that conatins [[GeoTiff]]s of type T.
 */
abstract class CropIterator[T <: CellGrid](geoTiff: GeoTiff[T],
  windowedCols: Int,
  windowedRows: Int) extends Iterator[GeoTiff[T]] {

  private val wholeCols: Int = geoTiff.imageData.cols
  private val wholeRows: Int = geoTiff.imageData.rows

  def colIterations: Int =
    if (wholeCols % windowedCols == 0)
      wholeCols / windowedCols
    else
      wholeCols / windowedCols + 1
  
  def rowIterations: Int =
    if (wholeRows % windowedRows == 0)
      wholeRows / windowedRows
    else
      wholeRows / windowedRows + 1

  var colCount: Int = 0
  var rowCount: Int = 0
 
  var colMin: Int = 0
  var rowMin: Int = 0

  var colMax: Int = math.min(windowedCols, wholeCols)
  var rowMax: Int = math.min(windowedRows, wholeRows)

  private var overallColCount: Int = 1
  private var overallRowCount: Int = 1
  
  def adjustValues: Unit = {
    if (colCount + 1 == colIterations) {
      colCount = 0
      rowCount += 1
      overallColCount += 1
      overallRowCount += 1
    } else {
      colCount += 1
      overallColCount += 1
    }

    colMin = math.min(windowedCols * colCount, wholeCols)
    rowMin = math.min(windowedRows * rowCount, wholeRows)

    colMax =
      math.min(windowedCols + (windowedCols * colCount), wholeCols)
    rowMax =
      math.min(windowedRows + (windowedRows * rowCount), wholeRows)
  }
  
  def hasNext: Boolean =
    if (overallColCount <= colIterations || overallRowCount <= rowIterations) {
      true
    } else {
      false
    }

  def next: GeoTiff[T]
}
