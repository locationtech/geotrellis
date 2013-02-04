package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

/** Computes the mean value of a neighborhood for a given raster. Returns a raster of TypeDouble
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 *
 * @note            If the neighborhood is a [[Square]] neighborhood, the mean calucation will use
 *                  the [[CellwiseMeanCalc]] to perform the calculation, because it is faster.
 *                  If the neighborhood is of any other type, then [[CursorMeanCalc]] is used.
 * 
 * @note            Mean does not currently support Double raster data inputs.
 *                  If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class Mean(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) =>
    n match {
      case Square(ext) => new CellwiseMeanCalc
      case _ => new CursorMeanCalc
    }
}) with HasAnalysisArea[Mean]

case class CellwiseMeanCalc() extends CellwiseCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Double = 0.0

 def add(r:Raster, x:Int, y:Int) = {
    val z = r.get(x,y)
    if (z != NODATA) {
      count += 1
      sum   += z
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val z = r.get(x,y)
    if (z != NODATA) {
      count -= 1
      sum -= z
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y, sum / count) }
  def reset() = { count = 0 ; sum = 0 }
}

case class CursorMeanCalc() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Double = 0.0

  def calc(r:Raster,c:Cursor) = {
    c.removedCells.foreach { (x,y) => 
      var v = r.get(x,y)
      if(v != NODATA) { count -= 1; sum -= v } 
    }
    c.addedCells.foreach { (x,y) => 
      var v = r.get(x,y)
      if(v != NODATA) { count += 1; sum += v } 
    }
    data.setDouble(c.col,c.row,sum / count)
  }
}
