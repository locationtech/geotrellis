package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.statistics._

case class RasterMoransI(r:Op[Raster], neighborhoodType: Neighborhood) extends Op1(r)({
  r => {
    val h = FastMapHistogram.fromRaster(r)
    val Statistics(mean, _, _, stddev, _, _) = h.generateStatistics
    val diff = r.convert(TypeDouble).force.mapDouble(_ - mean)
    FocalOp.getResultDouble(diff, Default,  neighborhoodType, () => new MoranCalc(stddev * stddev))
  }
})

protected[focal] class MoranCalc(stddevSquared: Double) extends FocalCalculation[Double] {
  var z:Double = 0.0
  var w:Int = 0
  var base:Double = 0.0
  var _row:Int = 0
  var _col:Int = 0
  override def center(col:Int, row:Int, _r:Raster) { _col = col; _row = row }
  def clear() { z = 0.0; w = 0 }
  def add(col:Int, row:Int, r:Raster) = if (col == _col && row == _row) {
    base = r.getDouble(col, row)
  } else {
    z += r.getDouble(col, row)
    w += 1
  }
  def remove(col:Int, row:Int, r:Raster) = sys.error("not supported")
  def getResult = (base / stddevSquared * z) / w
}

// Scalar version:

class NoopResultBuilder extends ResultBuilder[Double,Double] {
  def set(x:Int,y:Int,v:Double) = { }
  def build = 0.0
}

case class ScalarMoransI(r:Op[Raster], n:Neighborhood) extends Op1(r)({
  r => 
    val h = FastMapHistogram.fromRaster(r)
  val Statistics(mean,_,_,stddev,_,_) = h.generateStatistics
  val std2 = stddev*stddev

  var count:Double = 0.0
  var ws:Int = 0

  val diff = r.convert(TypeDouble).force.mapDouble(_ - mean)
  CursorStrategy.execute(diff, new NoopResultBuilder, Cursor.getDouble(diff,n)) {
    cursor =>
      var base = diff.getDouble(cursor.focusX,cursor.focusY)
      var z = -base

      for(v <- cursor) {
        z += v
        ws += 1
      }
      count += base / std2 * z
      ws -= 1 // for focus
      0
  }

  Result(count / ws)
})
