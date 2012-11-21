package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics._

case class Mode(r:Op[Raster], neighborhoodType: NeighborhoodType) extends Op1(r)({
  r => FocalOp.getResultInt(r, Default, neighborhoodType, () => new ModeCalc)
})

protected[focal] class ModeCalc extends FocalCalculation[Int] {
  var h:Histogram = FastMapHistogram()
  def clear() { h = FastMapHistogram() }
  def add(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), 1) }
  def remove(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), -1) }
  def getResult = h.getMode
}

