/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.raster._
import geotrellis.raster.op.focal.CellSet

/**
 * Downsamples the given raster to the given number of columns and rows
 * by applying the given function to the cells of the original raster
 * that compose a cell in the resulting raster.
 *
 * @note If the original RasterExtent dimensions are not divisible by the
 * given cols and rows, the CellSet argument of the function might give
 * invalid column and rows in it's foreach function. You need to guard
 * against this in the given function.
 *
 * @note    Currently only works with integer value functions.
 *
 * @example
 *<pre>
 * // Downsamples to a 4x3 raster according to the max value of the input raster.
 *
 * val op = Downsample(r, 4, 3)({
 *   cellSet =>
 *     var maxValue = Int.MinValue
 *     cellSet.foreach({ (col, row) => if(col < r.cols && row < r.rows) maxValue = math.max(r.get(col, row), maxValue) })
 *     maxValue
 * })
 * </pre> 
 */
case class Downsample(r: Tile, cols: Int, rows: Int)(f: CellSet=>Int)
     extends Op4(r, cols, rows, f)({
       (r, cols, rows, f) =>
         val colsPerBlock = math.ceil(r.cols / cols.toDouble).toInt
         val rowsPerBlock = math.ceil(r.rows / rows.toDouble).toInt  
         
         val tile = ArrayTile.empty(r.cellType, cols, rows)

         val cellSet = new DownsampleCellSet(colsPerBlock, rowsPerBlock)
         var col = 0
         while(col < cols) {
           var row = 0
           while(row < rows) {
             cellSet.focusOn(col, row)
             tile.set(col, row, f(cellSet))
             row += 1
           }
           col += 1
         }
         Result(tile)
})

class DownsampleCellSet(val colsPerBlock: Int, val rowsPerBlock: Int) extends CellSet {
  private var focusCol = 0
  private var focusRow = 0

  def focusOn(col: Int, row: Int) = {
    focusCol = col
    focusRow = row
  }
  
  def foreach(f: (Int, Int)=>Unit): Unit = {
    var col = 0
    while(col < colsPerBlock) {
      var row = 0      
      while(row < rowsPerBlock) {
        f(focusCol * colsPerBlock + col, focusRow * rowsPerBlock + row)
        row += 1
      }
      col += 1
    }
  }
}
