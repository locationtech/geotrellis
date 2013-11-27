package geotrellis.raster.op.hydrology

import geotrellis.raster.op.focal._
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
case class Fill(r:Op[Raster],tns:Op[TileNeighbors]) 
    extends FocalOp[Raster](r,Square(1),tns)({ 
      (r,n) =>
      if(r.isFloat) { new CursorFillCalcDouble }
      else { new CursorFillCalc }
    })

object Fill {
  def apply(r:Op[Raster]) = new Fill(r,TileNeighbors.NONE)
}

case class CursorFillCalc() extends CursorCalculation[Raster] with IntRasterDataResult {
  var count:Int = 0
  var totalCount:Int = 0
  var sum:Int = 0
  val thresh = 10

  def calc(r:Raster,c:Cursor) = {
    val cVal = r.get(c.col,c.row)
    c.removedCells.foreach { (col,row) => 
      if(col != c.col || row != c.row) {
        val v = r.get(col,row)
        if(isData(v)) {
          if((v - cVal).abs < thresh ){
            count -= 1
          }
          sum -= v
          totalCount -= 1
        }
      }
    }
    c.addedCells.foreach { (col,row) => 
      if(col != c.col || row != c.row) {
        val v = r.get(col,row)
        if(isData(v)) {
          if((v - cVal).abs < thresh ){
            count += 1
          }
          sum += v
          totalCount += 1
        }
      }
    }

    if(count == 0){
      data.set(c.col,c.row,((sum-cVal) / (totalCount-1))) 
    } else { 
      data.set(c.col,c.row,cVal)
    }
  }
}
case class CursorFillCalcDouble() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var count:Int = 0
  var totalCount:Int = 0
  var sum:Double = 0
  val thresh:Double = 10

  def calc(r:Raster,c:Cursor) = {
    val cVal = r.getDouble(c.col,c.row)

    c.removedCells.foreach { (col,row) => 
      if(col != c.row || row != c.col) {
        val v = r.getDouble(col,row)
        if(isData(v)) {
          if((v - cVal).abs < thresh ){
            count -= 1
          }
          sum -= v
          totalCount -= 1
        }
      }
    }

    c.addedCells.foreach { (col,row) => 
      if(col != c.col || row != c.row) {
        val v = r.getDouble(col,row)
        if(isData(v)) {
          if((v - cVal).abs < thresh ){
            count += 1
          }
          sum += v
          totalCount += 1
        }
      }
    }

    if(count == 0){
      data.setDouble(c.col,c.row,(sum-cVal) / (totalCount - 1) ) 
    } else { 
      data.setDouble(c.col,c.row,cVal)
    }
  }
}
