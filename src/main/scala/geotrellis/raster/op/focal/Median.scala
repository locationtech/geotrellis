package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics.FastMapHistogram

/** Computes the median value of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 */
case class Median(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult {
    def calc(r:Raster,cursor:Cursor) = {
      val h = FastMapHistogram()
      cursor.allCells.foreach { (x,y) => 
        val v = r.get(x,y)
        if(v != NODATA) { h.countItem(r.get(x,y),1) }
      }
      data.set(cursor.col,cursor.row,h.getMedian)
    }
  }
})
