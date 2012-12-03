package geotrellis.benchmark.oldfocal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class Min(r:Op[Raster], neighborhoodType: Neighborhood) extends Op1(r)({
  r => FocalOp.getResult(r, Default, neighborhoodType, MinFocalOpDef)
})

object MinFocalOpDef extends MultiTypeFocalOpDefinition {
  def newIntCalc = new IntMinCalc
  def newDoubleCalc = new DoubleMinCalc
}

class IntMinCalc extends FocalCalculation[Int] {
  var zmin = NODATA
  def clear() { zmin = NODATA }
  def add(col:Int, row:Int, r:Raster) { 
    if(zmin == NODATA) {
      zmin = r.get(col,row)
    } else {
      zmin = min(r.get(col, row), zmin) 
    }
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def getResult = zmin
}

class DoubleMinCalc extends FocalCalculation[Double] {
  var zmin = Double.NaN
  def clear() { zmin = Int.MaxValue }
  def add(col:Int, row:Int, r:Raster) { 
    if(zmin == Double.NaN) {
      zmin == r.getDouble(col, row)
    } else {
      zmin = min(r.getDouble(col, row), zmin)
    }
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def getResult = zmin
}
