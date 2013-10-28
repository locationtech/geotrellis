package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.TileNeighbors

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
case class Sum(r:Op[Raster], n:Op[Neighborhood],ns:Op[TileNeighbors]) extends FocalOp[Raster](r,n,ns)({ 
  (r,n) =>
    n match {
      case Square(ext) => new CellwiseSumCalc
      case _ => new CursorSumCalc
    }
})

object Sum {
  def apply(r:Op[Raster], n:Op[Neighborhood]) = new Sum(r,n,TileNeighbors.NONE)
}

class CursorSumCalc extends CursorCalculation[Raster] 
    with IntRasterDataResult {
  var total = 0

  def calc(r:RasterLike,cursor:Cursor) = {

    val added = collection.mutable.Set[(Int,Int,Int)]()
    cursor.addedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      added += ((x,y,v))
      if(v != NODATA) { total += r.get(x,y) }
    }

    val removed = collection.mutable.Set[(Int,Int,Int)]()
    cursor.removedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      removed += ((x,y,v))
      if(v != NODATA) { total -= r.get(x,y) }
    }

    data.set(cursor.col,cursor.row,total)
  }
}

class CellwiseSumCalc extends CellwiseCalculation[Raster]
    with IntRasterDataResult {
  var total = 0
  
  def add(r:RasterLike,x:Int,y:Int) = { 
    val v = r.get(x,y)
    if(v != NODATA) { total += r.get(x,y) }
  }

  def remove(r:RasterLike,x:Int,y:Int) = { 
    val v = r.get(x,y)
    if(v != NODATA) { total -= r.get(x,y) }
  }

  def reset() = { total = 0 }
  def setValue(x:Int,y:Int) = { 
    data.set(x,y,total) 
  }
}
