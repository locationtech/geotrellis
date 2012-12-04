package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class CursorMean(r:Op[Raster], n:Op[Neighborhood]) extends DoubleFocalOp[Raster](r,n) {
  var count:Int = 0
  var sum:Double = 0.0

  val removedCB = new DoubleFocalValueCB { 
    def apply(z:Double) = { if(z != Double.NaN) { count -= 1; sum -= z } }
  }

  val addedCB = new DoubleFocalValueCB { 
    def apply(z:Double) = { if(z != Double.NaN) { count += 1; sum += z } }
  }

  def calc(c:DoubleCursor):Double = {
    c.removedCells.foreach(removedCB)
    c.addedCells.foreach(addedCB)
    sum / count
  }

  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)
}

case class Mean(r:Op[Raster], n:Op[Neighborhood]) extends DoubleCellwiseFocalOp[Raster](r,n) {
  var count:Int = 0
  var sum:Double = 0.0

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

  def getValue = sum / count

  def reset() = { count = 0 ; sum = 0 }

  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)
}
