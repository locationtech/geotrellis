package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.macros._

class KernelCursor(r: Tile, kernel: Kernel, analysisArea: GridBounds)
    extends Cursor(r, analysisArea, kernel.extent)
    with MacroIterableTile
    with Serializable {
  private val ktileArr = kernel.tile.toArray
  private val ktileArrDouble = kernel.tile.toArrayDouble
  private val kcols = kernel.tile.cols

  def foreachWithWeight(f: (Int, Int, Int) => Unit): Unit =
    macro TileMacros.intForeach_impl

  def foreachWithWeightDouble(f: (Int, Int, Double) => Unit): Unit =
    macro TileMacros.doubleForeach_impl

  def foreachIntVisitor(f: IntTileVisitor): Unit = {
    var y = rowmin
    var x = 0
    while(y <= rowmax) {
      x = colmin
      while(x <= colmax) {
        val kcol = focusCol + extent - x
        val krow = focusRow + extent - y
        val w = ktileArr(krow * kcols + kcol)
        f(x, y, w)
        x += 1
      }
      y += 1
    }
  }

  def foreachDoubleVisitor(f: DoubleTileVisitor): Unit = {
    var y = rowmin
    var x = 0
    val ktile = kernel.tile
    while(y <= rowmax) {
      x = colmin
      while(x <= colmax) {
        val kcol = focusCol + extent - x
        val krow = focusRow + extent - y
        val w = ktileArrDouble(krow * kcols + kcol)
        f(x, y, w)
        x += 1
      }
      y += 1
    }
  }
}
