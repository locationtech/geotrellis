package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Max(r:Op[Raster], neighborhoodType: NeighborhoodType) extends Op1(r)({
  r => FocalOp.getResult(r, Default, neighborhoodType, MaxFocalOpDef)
})

protected[focal] object MaxFocalOpDef extends MultiTypeFocalOpDefinition {
  def newIntCalc = new IntMaxCalc
  def newDoubleCalc = new DoubleMaxCalc
}

protected[focal] class IntMaxCalc extends FocalCalculation[Int] {
  var zmax = NODATA
  def clear() { zmax = NODATA }
  def add(col:Int, row:Int, r:Raster) { 
    if(zmax == NODATA) { zmax = r.get(col,row) }
    else { zmax = max(r.get(col, row), zmax) }
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def getResult = zmax
}

protected[focal] class DoubleMaxCalc extends FocalCalculation[Double] {
  var zmax = Double.NaN
  def clear() { zmax = Double.NaN }
  def add(col:Int, row:Int, r:Raster) { 
    if(zmax == Double.NaN) { zmax = r.get(col,row) }
    else { zmax = max(r.get(col, row), zmax) }
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("remove() not supported")
  def getResult = zmax
}
