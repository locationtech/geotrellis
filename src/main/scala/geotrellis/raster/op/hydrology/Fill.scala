package geotrellis.raster.op.hydrology

import geotrellis.raster.op.focal._
import geotrellis._
import geotrellis.raster.TileNeighbors

import scala.math._

case class FillOptions(threshold:Double)
object FillOptions {
  val default = FillOptions(20.0)
}

/** Fills sink values in a raster. Returns a raster of TypeDouble
  *
  * @param    r         Raster on which to run the focal operation.
  * @param    options   FillOptions. This allows you to set the threshold appropriately.
  * @param    n         Neighborhood to use for this operation (e.g., [[Square]](1))
  * @param    tns       TileNeighbors that describe the neighboring tiles.

  * @return             Returns a double value raster that is the computed Fill for each neighborhood.
  * 
  * @note               The threshold in the options will be used to determine whether
  *                     or not something is a sink, so if you find that this operation
  *                     is incorrectly classifying your data, make sure you have set
  *                     the threshold appropriately.      
  */
case class Fill(r:Op[Raster],options:FillOptions,tns:Op[TileNeighbors])
    extends FocalOp1(r,Square(1),tns,options)({
      (r,n) =>
      if(r.isFloat) { new CursorFillCalcDouble }
      else { new CursorFillCalc }
    })

object Fill {
  def apply(r:Op[Raster]) = new Fill(r,FillOptions.default,TileNeighbors.NONE)
  def apply(r:Op[Raster],o:FillOptions) = new Fill(r,o,TileNeighbors.NONE)
}

case class CursorFillCalc() extends CursorCalculation[Raster]
                               with Initialization1[FillOptions]
                               with IntRasterDataResult {
  private var threshold:Int = 0

  def init(r:Raster,options:FillOptions) = {
    threshold = options.threshold.toInt
    super.init(r)
  }

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

case class CursorFillCalcDouble() extends CursorCalculation[Raster]
                                     with Initialization1[FillOptions]
                                     with DoubleRasterDataResult {
  private var threshold:Double = 0.0

    def init(r:Raster,options:FillOptions) = {
    threshold = options.threshold.toInt
    super.init(r)
  }

  def calc(r:Raster,c:Cursor) = {
    var count:Int = 0
    var totalCount:Int = 0
    var sum:Double = 0
    val cVal:Double = r.getDouble(c.col,c.row)
    c.allCells.foreach { (col,row) =>
      if(c.col != col || c.row != row){
        if((r.getDouble(col,row)-cVal).abs > threshold ){
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

