package geotrellis.op.raster.local

import geotrellis._
import geotrellis.op._

/**
 * Given a condition over two rasters, set the value of each cell in the output
 * to a specified true or false value after testing the specified condition on 
 * the corresponding values in each of the two input rasters.
 * each of the two input rasters.
 * 
 * Usage:
 * <pre>
 * // Generate a raster with the value 1 in each cell in which the value of A
 * // is greater than B in the corresponding cell.  Set the value to 0 if the 
 * // condition is false. 
 *
 * val C = BinaryIfElseCell(A,B, (a,b) => a > b, 1, 0) 
 * </pre> 
 */
case class BinaryIfElseCell(r1:Op[Raster], r2:Op[Raster],
                            cond: (Int,Int) => Boolean, trueValue:Int,
                            falseValue:Int) extends BinaryLocal {
  def handleCells(z1:Int, z2:Int): Int = if(cond(z1,z2)) trueValue else falseValue
}
