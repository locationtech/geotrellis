package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.statistics._

case class Mode(r:Op[Raster], f:Kernel) extends Op1(r)({
  r => Result(f.handle(r, new ModeStrategy(r)))
})

protected[focal] class ModeStrategy(r:Raster) extends Strategy[Raster, ModeCell](Aggregated) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:ModeCell) = d.set(col, row, cc.h.getMode)
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new ModeCell
}

protected[focal] class ModeCell extends Cell[ModeCell] {
  var h:Histogram = FastMapHistogram()
  def clear() { h = FastMapHistogram() }
  def add(cc:ModeCell) { h.update(cc.h) }
  def add(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), 1) }
  def remove(cc:ModeCell) = sys.error("unimplemented")
  def remove(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), -1) }
}
