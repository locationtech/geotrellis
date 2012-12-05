package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class Max(r:Op[Raster], n: Op[Neighborhood]) extends CursorFocalOp[Raster](r,n) {
  var data:IntArrayRasterData = null
  var rExtent:RasterExtent = null

  def init(r:Raster) = {
    rExtent = r.rasterExtent
    data = IntArrayRasterData.ofDim(rExtent.cols,rExtent.rows)
  }

  def calc(r:Raster, cursor:Cursor) = {
    var m = Int.MinValue
    cursor.allCells.foreach { (x,y) => m = max(m,r.get(x,y)) }
    data.set(cursor.focusX,cursor.focusY,m)
  }

  def getResult = Raster(data,rExtent)
}
