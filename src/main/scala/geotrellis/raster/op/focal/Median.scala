package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._
import geotrellis.statistics._

case class Median(r:Op[Raster], n:Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  def getCB(h:FastMapHistogram):IntFocalValueCB = 
    new IntFocalValueCB { def apply(v:Int) = { h.countItem(v,1) } }

  def calc(cursor:IntCursor) = {
    val h = FastMapHistogram()
    val cb = getCB(h)
    cursor.allCells.foreach(cb)
    h.getMedian
  }
}
