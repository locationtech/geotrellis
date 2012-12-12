package geotrellis.raster.op.focal

import geotrellis._

case class Sum(r:Op[Raster], n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({ 
  (r,n) =>
    n match {
      case Square(ext) => new CellwiseSumCalc
      case _ => new CursorSumCalc
    }
})

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

    data.set(cursor.focusX,cursor.focusY,total)
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
