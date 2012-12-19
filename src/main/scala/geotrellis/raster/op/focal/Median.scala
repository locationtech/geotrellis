package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics.FastMapHistogram

/** Computes the median value of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 */
case class Median(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => 
    n match {
      case Square(ext) => new CellwiseMedianCalc(ext)
      case _ => new CursorMedianCalc(n.extent)
    }
}) with HasAnaysisArea[Median]

class CursorMedianCalc(extent:Int) extends CursorCalculation[Raster] with IntRasterDataResult 
                                                                     with MedianModeCalculation {
  initArray(extent)
                                                         
  def calc(r:Raster,cursor:Cursor) = {
    cursor.removedCells.foreach { (x,y) =>
      val v = r.get(x,y)
      if(v != NODATA) {
        removeValue(v)
      }
    }
    cursor.addedCells.foreach { (x,y) =>
      val v = r.get(x,y)
      if(v != NODATA) addValueOrdered(v)
    }
    data.set(cursor.col,cursor.row,median)
  }
}

class CellwiseMedianCalc(extent:Int) extends CellwiseCalculation[Raster] with IntRasterDataResult 
                                                                         with MedianModeCalculation {
  initArray(extent)

  def add(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (v != NODATA) {
      addValueOrdered(v)
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (v != NODATA) {
      removeValue(v)
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y,median) }
}
