package geotrellis.raster.op.hydrology
import geotrellis.raster.op.focal
import geotrellis._
import geotrellis.raster.TileNeighbors

import scala.math._

/** Fills sink values in a raster. Returns a raster of TypeDouble
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.

 * @return          Returns a double value raster that is the computed Fill for each neighborhood.
 *
*/
case class Fill(r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) 
    extends FocalOp[Raster](r,n,tns)({
  (r,n) =>
      if(r.isFloat) {
        new CursorFillCalcDouble
      } else {
          new CursorFillCalc
      }
})

object Fill {
  def apply(r:Op[Raster],n:Op[Neighborhood]) = new Fill(r,n,TileNeighbors.NONE)
}

case class CursorFillCalc() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var totalCount:Int = 0
  var sum:Int = 0
  val thresh = 10

  def calc(r:Raster,c:Cursor) = {
    var cVal = r.get(c.col,c.row)
    c.removedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      if(v != NODATA) {
        if((v - cval).abs() < thresh ){
          count -= 1
        } 
        sum -= v
        totalCount -= 1
      } 
    }
    c.addedCells.foreach { (x,y) => 
      var cVal = r.get(c.col,c.row)
    c.removedCells.foreach { (x,y) => 
      val v = r.get(x,y)
      if(v != NODATA) {
        if((v - cval).abs < thresh ){
          count += 1
        } 
        sum += v
        totalCount += 1
      } 
    }
    if(count == 0){ data.setDouble(sum / totalCount.toDouble) }
    else { data.setDouble(cVal.toDouble }
    }
  }
}
case class CursorMeanCalcDouble() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var sum:Double = 0.0

  def calc(r:Raster,c:Cursor) = {
    c.removedCells.foreach { (x,y) => 
      val v = r.getDouble(x,y)
      if(!isNaN(v)) { count -= 1; sum -= v } 
    }
    c.addedCells.foreach { (x,y) => 
      val v = r.getDouble(x,y)
      if(!isNaN(v)) { count += 1; sum += v } 
    }
    data.setDouble(c.col,c.row,sum / count)
  }
}

