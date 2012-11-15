package geotrellis.raster.op.focal

import geotrellis._

case class Sum(r:Op[Raster], neighborhoodType: NeighborhoodType) extends Op1(r) ({
  r => FocalOp.getResult(r, Aggregated, neighborhoodType, SumFocalOpDef)
})

protected[focal] object SumFocalOpDef extends MultiTypeFocalOpDefinition {
  def newIntCalc = new IntSumCalc
  def newDoubleCalc = new DoubleSumCalc
}

protected[focal] class IntSumCalc extends FocalCalculation[Int] {
  var total = 0
  def clear() { total = 0 }
  def add(col:Int, row:Int, r:Raster) { total += r.get(col, row) }
  def remove(col:Int, row:Int, r:Raster) { total -= r.get(col, row) }
  def getResult = total
}

protected[focal] class DoubleSumCalc extends FocalCalculation[Double] {
  var total = 0.0
  def clear() { total = 0.0 }
  def add(col:Int, row:Int, r:Raster) { total += r.getDouble(col, row) }
  def remove(col:Int, row:Int, r:Raster) { total -= r.getDouble(col, row) }
  def getResult = total
}
