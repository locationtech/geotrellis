package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

case class Min(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult {
    def calc(r:Raster,cursor:Cursor) = {
      var m = Int.MaxValue
      cursor.allCells.foreach { (x,y) => m = min(m,r.get(x,y)) }
      data.set(cursor.focusX,cursor.focusY,m)
    }
  }
})
