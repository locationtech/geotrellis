package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import scala.math._

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @param    r      Raster on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Max(r:Op[Raster],n:Op[Neighborhood],tns:Op[TileNeighbors]) extends FocalOp(r,n,tns)({
  (r,n) => new CursorCalculation[Raster] with IntRasterDataResult { 
    def calc(r:Raster, cursor:Cursor) = {
      var m = Int.MinValue
      cursor.allCells.foreach { (x,y) => 
        val v = r.get(x,y)
        if(v > m) { m = v }
      }
      data.set(cursor.col,cursor.row,m)
    }
  }
})

object Max {
  def apply(r:Op[Raster],n:Op[Neighborhood]) = new Max(r,n,TileNeighbors.NONE)
}
