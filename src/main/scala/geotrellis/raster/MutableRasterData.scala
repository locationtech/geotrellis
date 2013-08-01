package geotrellis.raster

import geotrellis._

/**
 * MutableRasterData is a StrictRasterData whose cells can be written to
 * (mutated).
 */
trait MutableRasterData extends StrictRasterData {
  def mutable = Option(this)

  def update(i:Int, z:Int): Unit
  def updateDouble(i:Int, z:Double):Unit

  def set(col:Int, row:Int, value:Int) {
    update(row * cols + col, value)
  }
  def setDouble(col:Int, row:Int, value:Double) {
    updateDouble(row * cols + col, value)
  }
}
