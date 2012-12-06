package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

case class Sum(r:Op[Raster], n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({ 
  (r,n) =>
    n match {
      case Square(ext) => new CellwiseSumCalc
      case _ => new CursorSumCalc
    }
})

class CursorSumCalc extends CursorCalculation 
    with IntRasterDataResult {
  var total = 0

  def calc(r:Raster,cursor:Cursor) = {
    cursor.addedCells.foreach { (x,y) =>
      total += r.get(x,y)
    }

    cursor.removedCells.foreach { (x,y) => 
      total -= r.get(x,y)
    }

    data.set(cursor.focusX,cursor.focusY,total)
  }
}

class CellwiseSumCalc extends CellwiseCalculation
    with IntRasterDataResult {
  var total = 0
  
  def add(r:Raster,x:Int,y:Int) = { total += r.get(x,y) }
  def remove(r:Raster,x:Int,y:Int) = { total -= r.get(x,y) }
  def reset() = { total = 0 }
  def setValue(x:Int,y:Int) = data.set(x,y,total)
}
