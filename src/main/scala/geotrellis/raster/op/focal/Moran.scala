package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics.{Statistics,FastMapHistogram}

case class RasterMoransI(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new MoransCalc
})

case class MoransCalc() extends CursorCalculation[Raster] with DoubleRasterDataResult {
  var mean = 0.0
  var `stddev^2` = 0.0

  override def init(r:Raster) = {
    super.init(r)

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
}

// Scalar version:
case class ScalarMoransI(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp(r,n)({
  (r,n) => new CursorCalculation[Double] with Initialization {
    var mean:Double = 0
    var `stddev^2`:Double = 0

    var count:Double = 0.0
    var ws:Int = 0

    def init(r:Raster) = {
      val h = FastMapHistogram.fromRaster(r)
      val Statistics(m,_,_,s,_,_) = h.generateStatistics
      mean = m
      `stddev^2` = s*s
    }

    def calc(r:Raster,cursor:Cursor) = {
      var base = r.getDouble(cursor.focusX,cursor.focusY) - mean
      var z = -base

      cursor.allCells.foreach { (x,y) => z += r.getDouble(x,y) - mean; ws += 1 }

      count += base / `stddev^2` * z
      ws -= 1 // for focus     
    }

    def getResult = count / ws
  }
})
