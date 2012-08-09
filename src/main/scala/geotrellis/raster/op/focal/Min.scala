package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Min(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new MinContext(r)))
})

protected[focal] class MinContext(r:Raster) extends Context[Raster, MinCell](Columnar) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:MinCell) { d.set(col, row, cc.zmin) }
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new MinCell
}

protected[focal] class MinCell extends Cell[MinCell] {
  var zmin = Int.MaxValue
  def clear() { zmin = Int.MaxValue }
  def add(col:Int, row:Int, r:Raster) { zmin = min(r.get(col, row), zmin) }
  def add(cc:MinCell) { zmin = min(cc.zmin, zmin) }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def remove(z:MinCell) = sys.error("remove() not supported")
}
