package geotrellis.raster.op.transform

import geotrellis._
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
 * val op = Downsample(r,4,3)({
 *   cellSet =>
 *     var maxValue = Int.MinValue
 *     cellSet.foreach({ (col,row) => if(col < r.cols && row < r.rows) maxValue = math.max(r.get(col,row),maxValue) })
 *     maxValue
 * })
 * </pre> 
 */
case class Downsample(r: Raster, cols: Int, rows: Int)(f:CellSet=>Int)
     extends Op4(r,cols,rows,f)({
       (r,cols,rows,f) =>
         val colsPerBlock = math.ceil(r.cols / cols.toDouble).toInt
         val rowsPerBlock = math.ceil(r.rows / rows.toDouble).toInt
         val cellwidth = colsPerBlock * r.rasterExtent.cellwidth
         val cellheight = rowsPerBlock * r.rasterExtent.cellheight
         val xmin = r.rasterExtent.extent.xmin
         val ymin = r.rasterExtent.extent.ymin
         val ext = Extent(xmin,ymin,xmin + cellwidth*cols,ymin + cellheight*rows)
         val re = RasterExtent(ext,cellwidth,cellheight, cols, rows)
         
         val data = RasterData.emptyByType(r.data.getType, cols, rows)

         val cellSet = new DownsampleCellSet(colsPerBlock,rowsPerBlock)
         var col = 0
         while(col < cols) {
           var row = 0
           while(row < rows) {
             cellSet.focusOn(col,row)
             data.set(col,row, f(cellSet))
             row += 1
           }
           col += 1
         }
         Result(Raster(data,re))
})

class DownsampleCellSet(val colsPerBlock:Int, val rowsPerBlock:Int) extends CellSet {
  private var focusCol = 0
  private var focusRow = 0

  def focusOn(col:Int,row:Int) = {
    focusCol = col
    focusRow = row
  }
  
  def foreach(f:(Int,Int)=>Unit):Unit = {
    var col = 0
    while(col < colsPerBlock) {
      var row = 0      
      while(row < rowsPerBlock) {
        f(focusCol*colsPerBlock + col, focusRow*rowsPerBlock + row)
        row += 1
      }
      col += 1
    }
  }
}

