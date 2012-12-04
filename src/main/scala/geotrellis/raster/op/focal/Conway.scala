package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

case class Conway(r:Op[Raster]) extends IntFocalOp[Raster](r,Square(1)) {
  def createBuilder(r:Raster) = new ByteRasterBuilder(r.rasterExtent)

  var count = 0

  val addCB = new IntFocalValueCB { def apply(v:Int) = { if(v != NODATA) count += 1 } }
  val removedCB = new IntFocalValueCB { def apply(v:Int) = { if(v != NODATA) count -= 1 } }

  def calc(cursor:IntCursor) = {
    cursor.addedCells.foreach(addCB)
    cursor.removedCells.foreach(removedCB)
    if(count == 2 || count == 1) 1 else NODATA
  }
}
