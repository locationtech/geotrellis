package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

case class Min(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult {
    def calc(r:Raster,cursor:Cursor) = {
      var m = Int.MaxValue
      cursor.allCells.foreach { (col,row) => m = min(m,r.get(col,row)) }
      println(s"analysis coord: ${cursor.analysisCol},${cursor.analysisRow}")
      data.set(cursor.analysisCol,cursor.analysisRow,m)
    }
  }
})
