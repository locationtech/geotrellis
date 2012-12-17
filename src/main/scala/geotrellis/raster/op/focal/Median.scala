package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics.FastMapHistogram

case class Median(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult {
    def calc(r:Raster,cursor:Cursor) = {
      val h = FastMapHistogram()
      cursor.allCells.foreach { (x,y) => 
        val v = r.get(x,y)
        if(v != NODATA) { h.countItem(r.get(x,y),1) }
      }
      data.set(cursor.analysisCol,cursor.analysisRow,h.getMedian)
    }
  }
})
