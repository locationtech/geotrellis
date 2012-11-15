package geotrellis.raster.op.focal

import geotrellis._

case class Conway(r:Op[Raster]) extends Op1(r)({
  r => FocalOp.getResult(r, Aggregated, Square(1), ConwayFocalOpDef)
})

protected[focal] object ConwayFocalOpDef extends IntFocalOpDefinition {
  def newCalc = new ConwayCalc
  override def newData(r: Raster) = ByteFocalOpData(r.rasterExtent)
}

protected[focal] class ConwayCalc extends FocalCalculation[Int] {
  var count = 0
  def clear() { count = 0 }
  def add(col:Int, row:Int, r:Raster) { if (r.get(col, row) != NODATA) count += 1 }
  def remove(col:Int, row:Int, r:Raster) { if (r.get(col, row) != NODATA) count -= 1 }
  def getResult = if (count == 2 || count == 3) 1 else NODATA
}
