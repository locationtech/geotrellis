package geotrellis.benchmark.oldfocal

import scala.math._

import geotrellis._
import geotrellis.raster._
import geotrellis.statistics._

case class OldMedian(r:Op[Raster], neighborhoodType: Neighborhood) extends Op1(r)({
  r => FocalOp.getResultInt(r, Default, neighborhoodType, () => new MedianCalc)
})

class MedianCalc extends FocalCalculation[Int] {
  var h:Histogram = FastMapHistogram()
  def clear() { h = FastMapHistogram() }
  def add(col:Int, row:Int, r:Raster) { h.countItem(r.get(col, row), 1) }
  def remove(col:Int, row:Int, r:Raster) { sys.error("remove is not supported") }
  def getResult = h.getMedian
}
