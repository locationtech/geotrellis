package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.TileNeighbors

/** Computes the next step of Conway's Game of Life for a given [[Raster]].
 *
 * @param    r      Raster that represents a state of Conway's Game of Life,
 *                  where NODATA values are dead cells and any other value
 *                  is counted as alive cells.
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @see A description of Conway's Game of Life can be found on
 * [[http://en.wikipedia.org/wiki/Conway's_Game_of_Life wikipedia]].
 */
case class Conway(r:Op[Raster],tns:Op[TileNeighbors]) extends FocalOp[Raster](r,Square(1),tns)({
  (r,n) => new CellwiseCalculation[Raster] with ByteRasterDataResult {
    var count = 0

    def add(r:Raster, x:Int, y:Int) = {
      val z = r.get(x,y)
      if (z != NODATA) {
        count += 1
      }
    }

    def remove(r:Raster, x:Int, y:Int) = {
      val z = r.get(x,y)
      if (z != NODATA) {
        count -= 1
      }
    } 

    def setValue(x:Int,y:Int) = data.set(x,y, if(count == 3 || count == 2) 1 else NODATA)
    def reset() = { count = 0 }
  }
})
