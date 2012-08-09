package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Max(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new MaxContext(r)))
})

protected[focal] class MaxContext(r:Raster) extends Context[Raster](Columnar) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, z:Int) { d.set(col, row, z) }
  def get = Raster(d, r.rasterExtent)
  def makeCell() = new MaxCell
}

protected[focal] class MaxCell extends Cell {
  var zmax = Int.MinValue
  def clear() { zmax = Int.MinValue }
  def add(z:Int) { zmax = max(z, zmax) }
  def remove(z:Int) = sys.error("remove() not supported")
  def get() = zmax
  def copy() = new MaxCell
}
