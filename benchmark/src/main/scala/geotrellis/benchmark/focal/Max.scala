package geotrellis.benchmark.oldfocal

import scala.math._

import geotrellis._
import geotrellis.raster._

case class OldMax(r:Op[Raster], neighborhoodType: Neighborhood) extends Op1(r)({
  r => FocalOp.getResult(r, Default, neighborhoodType, MaxFocalOpDef)
})

object MaxFocalOpDef extends MultiTypeFocalOpDefinition {
  def newIntCalc = new IntMaxCalc
  def newDoubleCalc = new DoubleMaxCalc
}

class IntMaxCalc extends FocalCalculation[Int] {
  var zmax = NODATA
  def clear() { zmax = NODATA }
  def add(col:Int, row:Int, r:Raster) { 
    if(zmax == NODATA) { zmax = r.get(col,row) }
    else { zmax = max(r.get(col, row), zmax) }
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def getResult = zmax
}

class DoubleMaxCalc extends FocalCalculation[Double] {
  var zmax = Double.NaN
  def clear() { zmax = Double.NaN }
  def add(col:Int, row:Int, r:Raster) { 
    if(zmax == Double.NaN) { zmax = r.get(col,row) }
    else { zmax = max(r.get(col, row), zmax) }
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def getResult = zmax
}
