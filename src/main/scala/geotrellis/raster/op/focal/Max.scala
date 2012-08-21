package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Max(r:Op[Raster], f:Kernel) extends Op1(r)({
  r => Result(f.handle(r, new MaxStrategy(r)))
})

protected[focal] class MaxStrategy(r:Raster) extends Strategy[Raster, MaxCell](Columnar) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:MaxCell) { d.set(col, row, cc.zmax) }
  def get = Raster(d, r.rasterExtent)
  def makeCell() = new MaxCell
}

protected[focal] class MaxCell extends Cell[MaxCell] {
  var zmax = Int.MinValue
  def clear() { zmax = Int.MinValue }
  def add(cc:MaxCell) { zmax = max(cc.zmax, zmax) }
  def add(col:Int, row:Int, r:Raster) { zmax = max(r.get(col, row), zmax) }
  def remove(z:MaxCell) = sys.error("remove() not supported")
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def copy() = new MaxCell
}
