package geotrellis.raster.op.focal

import geotrellis.raster._

class KernelCursor(r: Tile, kernel: Kernel, analysisArea: GridBounds) extends Cursor(r, analysisArea, kernel.extent) {
  def foreachWithWeight(f: (Int, Int, Double) => Unit): Unit = {
    var y = rowmin
    var x = 0
    while(y <= rowmax) {
      x = colmin
      while(x <= colmax) {
        f(x, y, kernel.tile.getDouble(focusCol + extent - x, focusRow + extent - y))
        x += 1
      }
      y += 1
    }
  }
}
