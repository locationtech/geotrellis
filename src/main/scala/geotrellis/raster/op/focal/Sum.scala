package geotrellis.raster.op.focal

import geotrellis._

case class Sum(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new SumContext(r)))
})

protected[focal] class SumContext(r:Raster) extends Context[Raster, SumCell](Aggregated) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, z:Int) { d.set(col, row, z) }
  def get() = Raster(d, r.rasterExtent)
  def makeCell():Cell = new SumCell
}

protected[focal] class SumCell extends Cell {
  var total = 0
  def clear() { total = 0 }
  def get() = total
  def add(z:Int) { total += z }
  def remove(z:Int) { total -= z }
}
