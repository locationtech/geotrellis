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
  
    val threshold:Int = 20

    def calc(r:Raster,c:Cursor) = {
      var count:Int = 0
      var totalCount:Int = 0
      var sum:Int = 0
      val cVal = r.get(c.col,c.row)
      c.allCells
      .foreach { (col,row) => 
        if(c.col != col || c.row != row){
          if((r.get(col,row)-cVal).abs > threshold ){
            count = count + 1 
          }
          totalCount = totalCount + 1
            sum = sum + r.get(col,row)
        }

      }

      if(count == totalCount){
          data.set(c.col,c.row, sum/totalCount) 
      } else { 
        data.set(c.col,c.row,cVal)
      }
    }
}
case class CursorFillCalcDouble() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  val thresh:Double = 20.0

    def calc(r:Raster,c:Cursor) = {
      var count:Int = 0
      var totalCount:Int = 0
      var sum:Double = 0
      val cVal:Double = r.getDouble(c.col,c.row)
      c.allCells.foreach { (col,row) => 
        if(c.col != col || c.row != row){
          if((r.getDouble(col,row)-cVal).abs > thresh ){
            count = count + 1 
          }
          totalCount = totalCount + 1
            sum = sum + r.get(col,row)
        }
      }
      if(count == totalCount){
          data.setDouble(c.col,c.row, sum/totalCount) 
      } else { 
        data.setDouble(c.col,c.row,cVal)
      }
    }
}

