package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

case class Conway(r:Op[Raster]) extends CellwiseFocalOp[Raster](r,Square(1)) {
  var data:ByteArrayRasterData = null 
  var rasterExtent:RasterExtent = null

  var count = 0

  def init(r:Raster) = {
    rasterExtent = r.rasterExtent
    data = ByteArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  }

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

  def setValue(x:Int,y:Int) = data.set(x,y, if(count == 2 || count == 1) 1 else NODATA)

  def reset() = { count = 0 }

  def getResult = Raster(data, rasterExtent)
}
