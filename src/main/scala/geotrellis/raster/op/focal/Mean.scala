package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Mean(r:Op[Raster], f:Focus) extends Op1(r)({
  r => Result(f.handle(r, new MeanContext(r)))
})

protected[focal] class MeanContext(r:Raster) extends Context[Raster, MeanCell](Aggregated) {
  val d = IntArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:MeanCell) { d.set(col, row, cc.get()) }
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new MeanCell
}

protected[focal] class MeanCell extends Cell {
  var total:Double = 0.0
  var count:Int = 0
  def clear() { total = 0.0; count = 0 }
  def add(z:Int) { total += z; count += 1 }
  def remove(z:Int) { total -= z; count -= 1 }
  def get() = round(total / count).toInt
}
