package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.TileNeighbors
import geotrellis.statistics.FastMapHistogram

/** Computes the median value of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @note    Median does not currently support Double raster data.
 *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *          the data values will be rounded to integers.
 */
case class Median(r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) 
    extends FocalOp[Raster](r,n,tns)({
  (r,n) => 
    n match {
      case Square(ext) => new CellwiseMedianCalc(ext)
      case _ => new CursorMedianCalc(n.extent)
    }
})

object Median {
  def apply(r:Op[Raster],n:Op[Neighborhood]) = new Median(r,n,TileNeighbors.NONE)
}

class CursorMedianCalc(extent:Int) extends CursorCalculation[Raster] with IntRasterDataResult 
                                                                     with MedianModeCalculation {
  initArray(extent)
                                                         
  def calc(r:Raster,cursor:Cursor) = {
    cursor.removedCells.foreach { (x,y) =>
      val v = r.get(x,y)
      if(isData(v)) {
        removeValue(v)
      }
    }
    cursor.addedCells.foreach { (x,y) =>
      val v = r.get(x,y)
      if(isData(v)) addValueOrdered(v)
    }
    data.set(cursor.col,cursor.row,median)
  }
}

class CellwiseMedianCalc(extent:Int) extends CellwiseCalculation[Raster] with IntRasterDataResult 
                                                                         with MedianModeCalculation {
  initArray(extent)

  def add(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (isData(v)) {
      addValueOrdered(v)
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (isData(v)) {
      removeValue(v)
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y,median) }
}
