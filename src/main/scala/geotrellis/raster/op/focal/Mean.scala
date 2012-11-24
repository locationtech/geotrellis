package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class CursorMean(r:Op[Raster], n:Neighborhood) extends DoubleCursorFocalOp1(r,n) {
  var count:Int = 0
  var sum:Double = 0.0

  def calc(c:Cursor[Double]):Double = {
    c.foreachOld { z =>
      count -= 1
      sum -= z
    }
    c.foreachNew { z =>
      count += 1
      sum += z
    }
    sum / count
  }
}

case class Mean(r:Op[Raster], n:Neighborhood) extends DoubleCellwiseFocalOp1(r,n) {
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
}
