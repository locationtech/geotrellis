package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.statistics._

case class Median(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new MedianContext(r)))
})

protected[focal] class MedianContext(r:Raster) extends Context[Raster, MedianCell](Aggregated) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:MedianCell) { d.set(col, row, cc.get()) }
  def get = Raster(d, r.rasterExtent)
  def makeCell() = new MedianCell
}

protected[focal] class MedianCell extends Cell[MedianCell] {
  var h:Histogram = FastMapHistogram()
  def clear() { h = FastMapHistogram() }
  def add(cc:MedianCell) { h.update(cc.h) }
  def add(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), 1) }
  def remove(cc:MedianCell) = sys.error("unimplemented")
  def remove(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), -1) }
  def get() = h.getMedian()
}
