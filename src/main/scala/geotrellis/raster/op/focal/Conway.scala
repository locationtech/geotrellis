package geotrellis.raster.op.focal

import geotrellis._

case class Conway(r:Op[Raster]) extends Op1(r)({
  r => Result(Square(1).handle(r, new ConwayContext(r)))
})

protected[focal] class ConwayContext(r:Raster) extends Context[Raster, ConwayCell](Aggregated) {
  val d = ByteArrayRasterData.ofDim(r.cols, r.rows)
  def store(col:Int, row:Int, cc:ConwayCell) = {
    d.set(col, row, if (cc.count == 2 || cc.count == 3) 1 else NODATA)
  }
  def get() = Raster(d, r.rasterExtent)
  def makeCell() = new ConwayCell
}

protected[focal] class ConwayCell extends Cell[ConwayCell] {
  var count = 0
  def clear() { count = 0 }
  def add(cc:ConwayCell) { count += cc.count }
  def add(col:Int, row:Int, r:Raster) { if (r.get(col, row) != NODATA) count += 1 }
  def remove(cc:ConwayCell) { count -= cc.count }
  def remove(col:Int, row:Int, r:Raster) { if (r.get(col, row) != NODATA) count -= 1 }
}
