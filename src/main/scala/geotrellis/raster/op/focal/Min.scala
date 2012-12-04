package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class Min(r:Op[Raster], n:Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  def calc(cursor:IntCursor):Int= {
    cursor.allCells.foldLeft(Int.MaxValue) { (a,b) => min(a,b) }
  }
}
