package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class Max(r:Op[Raster], n: Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  val folder = new IntFocalFoldCB { def apply(a:Int,v:Int) = max(a,v) }

  def calc(cursor:IntCursor) = {
    cursor.allCells.foldLeft(Int.MinValue)(folder)
  }
}
