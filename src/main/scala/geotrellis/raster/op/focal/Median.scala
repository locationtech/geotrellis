package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.statistics._

case class Median(r:Op[Raster], neighborhoodType: NeighborhoodType) extends Op1(r)({
  r => FocalOp.getResultInt(r, Default, neighborhoodType, () => new MedianCalc)
})

protected[focal] class MedianCalc extends FocalCalculation[Int] {
  var h:Histogram = FastMapHistogram()
  def clear() { h = FastMapHistogram() }
  def add(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), 1) }
  def remove(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), -1) }
  def getResult = h.getMedian
}
