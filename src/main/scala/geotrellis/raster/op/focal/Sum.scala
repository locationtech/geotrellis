package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

case class Sum(r:Op[Raster], n:Op[Neighborhood]) extends CursorFocalOp[Raster](r,n) {
  var data:IntArrayRasterData = null
  var rExtent:RasterExtent = null

  def init(r:Raster) = {
    rExtent = r.rasterExtent
    data = IntArrayRasterData.ofDim(rExtent.cols,rExtent.rows)
  }

  var total = 0
  def calc(r:Raster,cursor:Cursor) = {
    cursor.addedCells.foreach { (x,y) =>
      total += r.get(x,y)
    }

    cursor.removedCells.foreach { (x,y) => 
      total -= r.get(x,y)
    }

    data.set(cursor.focusX,cursor.focusY,total)
  }

  def getResult = Raster(data,rExtent)
}
