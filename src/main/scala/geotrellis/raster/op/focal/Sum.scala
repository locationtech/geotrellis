package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

case class Sum(r:Op[Raster], n:Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  var total = 0

  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  def calc(cursor:Cursor[Int]) = {
    for(v <- cursor.addedCells) { total += v }
    for(v <- cursor.removedCells) { total -= v }
    total
  }
}
