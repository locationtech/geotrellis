package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

/** Computes the minimum value of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 */
case class Min(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult {
    def calc(r:Raster,cursor:Cursor) = {
      var m = Int.MaxValue
      cursor.allCells.foreach { (x,y) => m = min(m,r.get(x,y)) }
      data.set(cursor.col,cursor.row,m)
    }
  }
})
