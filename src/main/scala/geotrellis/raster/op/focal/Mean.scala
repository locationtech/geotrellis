package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class CursorMean(r:Op[Raster], n:Op[Neighborhood]) extends CursorFocalOp[Raster](r,n) {
  var data:DoubleArrayRasterData = null
  var rExtent:RasterExtent = null

  var count:Int = 0
  var sum:Double = 0.0

  def init(r:Raster) = {
    rExtent = r.rasterExtent
    data = DoubleArrayRasterData.ofDim(rExtent.cols,rExtent.rows)
  }

  def calc(r:Raster,c:Cursor) = {
    c.removedCells.foreach { (x,y) => 
      var v = r.get(x,y)
      if(v != Double.NaN) { count -= 1; sum -= v } 
    }
    c.addedCells.foreach { (x,y) => 
      var v = r.get(x,y)
      if(v != Double.NaN) { count += 1; sum += v } 
    }
    data.setDouble(c.focusX,c.focusY,sum / count)
  }

  def getResult = Raster(data,rExtent)
}

case class Mean(r:Op[Raster], n:Op[Neighborhood]) extends CellwiseFocalOp[Raster](r,n) {
  var rExtent:RasterExtent = null
  var data:DoubleArrayRasterData=null

  var count:Int = 0
  var sum:Double = 0.0

  def init(r:Raster) = {
    rExtent = r.rasterExtent
    data = DoubleArrayRasterData.ofDim(rExtent.cols, rExtent.rows)
  }

  def add(r:Raster, x:Int, y:Int) = {
    val z = r.get(x,y)
    if (z != NODATA) {
      count += 1
      sum   += z
    }
  }

  def remove(r:Raster, x:Int, y:Int) = {
    val z = r.get(x,y)
    if (z != NODATA) {
      count -= 1
      sum -= z
    }
  } 

  def setValue(x:Int,y:Int) = { data.setDouble(x,y, sum / count) }

  def reset() = { count = 0 ; sum = 0 }

  def getResult = Raster(data,rExtent)
}
