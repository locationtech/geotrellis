package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.statistics._

case class Mode(r:Op[Raster],n:Op[Neighborhood]) extends CursorFocalOp[Raster](r,n) {
  var data:IntArrayRasterData = null
  var rExtent:RasterExtent = null

  def init(r:Raster) = {
    rExtent = r.rasterExtent
    data = IntArrayRasterData.ofDim(rExtent.cols,rExtent.rows)
  }

  def calc(r:Raster,cursor:Cursor) = {
    val h = FastMapHistogram()
    cursor.allCells.foreach { (x,y) => h.countItem(r.get(x,y),1) }      
    data.set(cursor.focusX,cursor.focusY,h.getMode)
  }

  def getResult = Raster(data,rExtent)
}
