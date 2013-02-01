package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

/** Computes the maximum value of a neighborhood for a given raster 
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Max(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp(r,n)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult { 
    def calc(r:Raster, cursor:Cursor) = {
      var m = Int.MinValue
      cursor.allCells.foreach { (x,y) => m = max(m,r.get(x,y)) }
      data.set(cursor.col,cursor.row,m)
    }
  }
}) with HasAnalysisArea[Max]
