package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Min(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new MinContext(r)))
})

protected[focal] class MinContext(r:Raster) extends Context[Raster](Columnar) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, z:Int) { d.set(col, row, z) }
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new MinCell
}

protected[focal] class MinCell extends Cell {
  var zmin = Int.MaxValue
  def clear() { zmin = Int.MaxValue }
  def add(z:Int) { zmin = min(z, zmin) }
  def remove(z:Int) = sys.error("remove() not supported")
  def get() = zmin
}
