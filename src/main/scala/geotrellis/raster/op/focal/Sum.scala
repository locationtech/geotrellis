package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

case class Sum(r:Op[Raster], n:Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  var total = 0

  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  val addedCB = new IntFocalValueCB { def apply(v:Int) = { total += v } }
  val removedCB = new IntFocalValueCB { def apply(v:Int) = { total -= v } }

  def calc(cursor:IntCursor) = {
    cursor.addedCells.foreach(addedCB)
    cursor.removedCells.foreach(removedCB)
    total
  }
}
