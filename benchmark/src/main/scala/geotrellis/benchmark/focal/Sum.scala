package geotrellis.benchmark.oldfocal

import geotrellis._
import geotrellis.raster._

case class Sum(r:Op[Raster], neighborhoodType: Neighborhood) extends Op1(r) ({
  r => FocalOp.getResult(r, Aggregated, neighborhoodType, SumFocalOpDef)
})

 object SumFocalOpDef extends MultiTypeFocalOpDefinition {
  def newIntCalc = new IntSumCalc
  def newDoubleCalc = new DoubleSumCalc
}

class IntSumCalc extends FocalCalculation[Int] {
  var total = 0
  def clear() { total = 0 }
  def add(col:Int, row:Int, r:Raster) { total += r.get(col, row) }
  def remove(col:Int, row:Int, r:Raster) { total -= r.get(col, row) }
  def getResult = total
}

class DoubleSumCalc extends FocalCalculation[Double] {
  var total = 0.0
  def clear() { total = 0.0 }
  def add(col:Int, row:Int, r:Raster) { total += r.getDouble(col, row) }
  def remove(col:Int, row:Int, r:Raster) { total -= r.getDouble(col, row) }
  def getResult = total
}
