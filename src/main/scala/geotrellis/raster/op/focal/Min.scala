package geotrellis.raster.op.focal

import scala.math._

import geotrellis._

case class Min2(r:Op[Raster], n:Neighborhood) extends IntCursorFocalOp1(r,n) {
  def calc(cursor:Cursor[Int]):Int= {
    cursor.foldLeft(Int.MaxValue) { (a,b) => min(a,b) }
  }
}

case class Min(r:Op[Raster], neighborhoodType: Neighborhood) extends Op1(r)({
  r => FocalOp.getResult(r, Default, neighborhoodType, MinFocalOpDef)
})

protected[focal] object MinFocalOpDef extends MultiTypeFocalOpDefinition {
  def newIntCalc = new IntMinCalc
  def newDoubleCalc = new DoubleMinCalc
}

protected[focal] class IntMinCalc extends FocalCalculation[Int] {
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

protected[focal] class DoubleMinCalc extends FocalCalculation[Double] {
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
