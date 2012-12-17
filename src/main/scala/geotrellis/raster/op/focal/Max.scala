package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

case class Max(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp(r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult { 
    def calc(r:Raster, cursor:Cursor) = {
      var m = Int.MinValue
      cursor.allCells.foreach { (x,y) => m = max(m,r.get(x,y)) }
      data.set(cursor.col,cursor.row,m)
    }
  }
})
