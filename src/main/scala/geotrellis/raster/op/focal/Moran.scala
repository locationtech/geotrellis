package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._

case class ScalarMoransI(r:Op[Raster], f:Focus) extends Op1(r)({
  r => {
    val h = FastMapHistogram.fromRaster(r)
    val Statistics(mean, _, _, stddev, _, _) = h.generateStatistics
    val v:Double = stddev * stddev
    val diff = r.convert(TypeDouble).force.mapDouble(_ - mean)
    Result(f.handle(diff, new ScalarMoranContext(v)))
  }
})

protected[focal] class ScalarMoranContext(v:Double) extends Context[Double, MoranCell](Default) {
  var count:Double = 0.0
  var ws:Int = 0
  def store(col:Int, row:Int, cc:MoranCell) {
    count += cc.base / v * cc.z
    ws += cc.w
  }
  def get() = count / ws
  def makeCell() = new MoranCell
}

case class RasterMoransI(r:Op[Raster], f:Focus) extends Op1(r)({
  r => {
    val h = FastMapHistogram.fromRaster(r)
    val Statistics(mean, _, _, stddev, _, _) = h.generateStatistics
    val v:Double = stddev * stddev
    val diff = r.convert(TypeDouble).force.mapDouble(_ - mean)
    Result(f.handle(diff, new RasterMoranContext(r.rasterExtent, v)))
  }
})

protected[focal] class RasterMoranContext(re:RasterExtent, v:Double) extends Context[Raster, MoranCell](Default) {
  val d = DoubleArrayRasterData.ofDim(re.cols, re.rows)
  def store(col:Int, row:Int, cc:MoranCell) {
    d.setDouble(col, row, (cc.base / v * cc.z) / cc.w)
  }
  def get() = Raster(d, re)
  def makeCell() = new MoranCell
}

protected[focal] class MoranCell extends Cell[MoranCell] {
  var z:Double = 0.0
  var w:Int = 0
  var base:Double = 0.0
  var _row:Int = 0
  var _col:Int = 0
  override def center(col:Int, row:Int, _r:Raster) { _col = col; _row = row }
  def clear() { z = 0.0; w = 0 }
  def add(cc:MoranCell) = sys.error("not supported")
  def add(col:Int, row:Int, r:Raster) = if (col == _col && row == _row) {
    base = r.getDouble(col, row)
  } else {
    z += r.getDouble(col, row)
    w += 1
  }
  def remove(cc:MoranCell) = sys.error("not supported")
  def remove(col:Int, row:Int, r:Raster) = sys.error("not supported")
}
