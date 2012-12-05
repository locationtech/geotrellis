package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.statistics._

case class RasterMoransI(r:Op[Raster],n:Op[Neighborhood]) extends CursorFocalOp[Raster](r,n) {
  var data:DoubleArrayRasterData = null
  var rExtent:RasterExtent = null

  var mean = 0.0
  var `stddev^2` = 0.0

  override def init(r:Raster) = {
    rExtent = r.rasterExtent
    data = DoubleArrayRasterData.ofDim(rExtent.cols,rExtent.rows)

    val h = FastMapHistogram.fromRaster(r)
    val Statistics(m,_,_,s,_,_) = h.generateStatistics
    mean = m
    `stddev^2` = s*s
  }

  def calc(r:Raster,cursor:Cursor) = {
    var z = 0.0
    var w = 0
    var base = 0.0

    cursor.allCells.foreach { (x,y) =>
      if(x == cursor.focusX && y == cursor.focusY) {
        base = r.getDouble(x,y)-mean
      } else {
        z += r.getDouble(x,y)-mean
        w += 1
      }
    }

    data.setDouble(cursor.focusX,cursor.focusY,(base / `stddev^2` * z) / w)
  }

  def getResult = Raster(data,rExtent)
}

// Scalar version:
case class ScalarMoransI(r:Op[Raster], n:Neighborhood) extends Op1(r)({
  r => 
    val h = FastMapHistogram.fromRaster(r)
    val Statistics(mean,_,_,stddev,_,_) = h.generateStatistics
    val std2 = stddev*stddev

    var count:Double = 0.0
    var ws:Int = 0

    val diff = r.convert(TypeDouble).force.mapDouble(_ - mean)
    CursorStrategy.execute(diff, Cursor(diff,n)) { (r,cursor) =>
      var base = diff.getDouble(cursor.focusX,cursor.focusY)
      var z = -base

      cursor.allCells.foreach { (x,y) => z += r.getDouble(x,y) ; ws += 1 }

      count += base / std2 * z
      ws -= 1 // for focus
      0
    }


    Result(count / ws)
})
