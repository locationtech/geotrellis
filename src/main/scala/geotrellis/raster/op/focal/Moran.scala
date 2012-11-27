package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._

//  TODO: Replace the Moran's I Scalar with something that isn't focal, it's not a focal op.

// case class ScalarMoransI(r:Op[Raster], f:Kernel[Double]) extends Op1(r)({
//   r => {
//     val h = FastMapHistogram.fromRaster(r)
//     val Statistics(mean, _, _, stddev, _, _) = h.generateStatistics
//     val v:Double = stddev * stddev
//     val diff = r.convert(TypeDouble).force.mapDouble(_ - mean)
//     Result(f.handle(diff, new ScalarMoranStrategy(v), () => new MoranCell))
//   }
// })

// protected[focal] class ScalarMoranStrategy(v:Double) extends Strategy[Double, MoranCell](Default) {
//   var count:Double = 0.0
//   var ws:Int = 0
//   def store(col:Int, row:Int, cc:MoranCell) {
//     count += cc.base / v * cc.z
//     ws += cc.w
//   }
//   def get() = count / ws
// }

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
