package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class Min(r:Op[Raster], n:Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  def folder = new IntFocalFoldCB { def apply(a:Int,v:Int) = min(a,v) }

  def calc(cursor:IntCursor):Int= {
    cursor.allCells.foldLeft(Int.MaxValue)(folder)
  }
}
