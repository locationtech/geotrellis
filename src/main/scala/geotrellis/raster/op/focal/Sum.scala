package geotrellis.raster.op.focal

import geotrellis._

/** Computes the sum of values of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 *
 * @note            If the neighborhood is a [[Square]] neighborhood, the sum calucation will use
 *                  the [[CellwiseSumCalc]] to perform the calculation, because it is faster.
 *                  If the neighborhood is of any other type, then [[CursorSumCalc]] is used.
 *
 * @note            Sum does not currently support Double raster data.
 *                  If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class Sum(r:Op[Raster], n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({ 
  (r,n) =>
    n match {
      case Square(ext) => new CellwiseSumCalc
      case _ => new CursorSumCalc
    }
}) with CanTile

class CursorSumCalc extends CursorCalculation[Raster] 
    with IntRasterDataResult {
  var total = 0

  def calc(r:Raster,cursor:Cursor) = {
    cursor.addedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      if(v != NODATA) { total += r.get(x,y) }
    }
    cursor.removedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      if(v != NODATA) { total -= r.get(x,y) }
    }

    data.set(cursor.col,cursor.row,total)
  }
}

class CellwiseSumCalc extends CellwiseCalculation[Raster]
    with IntRasterDataResult {
  var total = 0
  
  def add(r:Raster,x:Int,y:Int) = { 
    val v = r.get(x,y)
    if(v != NODATA) { total += r.get(x,y) }
  }

  def remove(r:Raster,x:Int,y:Int) = { 
    val v = r.get(x,y)
    if(v != NODATA) { total -= r.get(x,y) }
  }

  def reset() = { total = 0 }
  def setValue(x:Int,y:Int) = data.set(x,y,total)
}
