package geotrellis.raster.op.focal

import geotrellis._

case class Sum(r:Op[Raster], f:Kernel) extends Op1(r)({
  r => Result(f.handle(r, new SumStrategy(r)))
})

protected[focal] class SumStrategy(r:Raster) extends Strategy[Raster, SumCell](Aggregated) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:SumCell) = d.set(col, row, cc.total)
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new SumCell
}

protected[focal] class SumCell extends Cell[SumCell] {
  var total = 0
  def clear() { total = 0 }
  def add(cc:SumCell) { total += cc.total }
  def add(col:Int, row:Int, r:Raster) { total += r.get(col, row) }
  def remove(cc:SumCell) { total -= cc.total }
  def remove(col:Int, row:Int, r:Raster) { total -= r.get(col, row) }
}
