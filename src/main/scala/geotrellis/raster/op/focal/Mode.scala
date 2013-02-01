package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.statistics.FastMapHistogram

/** Computes the mode of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @note            Mode does not currently support Double raster data.
 *                  If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                  the data values will be rounded to integers.
 */
case class Mode(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => 
    n match {
      case Square(ext) => new CellwiseModeCalc(ext)
      case _ => new CursorModeCalc(n.extent)
    }
}) with HasAnalysisArea[Mode]

class CursorModeCalc(extent:Int) extends CursorCalculation[Raster] with IntRasterDataResult 
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
      if(v != NODATA) addValue(v)
    }
    data.set(cursor.col,cursor.row,mode)
  }
}

class CellwiseModeCalc(extent:Int) extends CellwiseCalculation[Raster] with IntRasterDataResult 
                                                                       with MedianModeCalculation {
  initArray(extent)

  def add(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (v != NODATA) {
      addValue(v)
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val v = r.get(x,y)
    if (v != NODATA) {
      removeValue(v)
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y,mode) }
}
