package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class Max(r:Op[Raster], n: Op[Neighborhood]) extends IntFocalOp[Raster](r,n) {
  def createBuilder(r:Raster) = new IntRasterBuilder(r.rasterExtent)

  def calc(cursor:Cursor[Int]) = {
    cursor.foldLeft(Int.MinValue) { (a,v) => max(a,v) }
  }
}
