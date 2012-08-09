package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.statistics._

case class Mode(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new ModeContext(r)))
})

protected[focal] class ModeContext(r:Raster) extends Context[Raster](Aggregated) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, z:Int) { d.set(col, row, z) }
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new ModeCell
}

protected[focal] class ModeCell extends Cell {
  var h:Histogram = FastMapHistogram()
  def clear() { h = FastMapHistogram() }
  def add(z:Int) { h.countItem(z, 1) }
  def remove(z:Int) { h.countItem(z, -1) }
  def get() = h.getMode()
}
