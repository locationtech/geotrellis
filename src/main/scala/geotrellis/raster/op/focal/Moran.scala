package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.statistics._

case class RasterMoransI(r:Op[Raster],n:Op[Neighborhood]) extends DoubleFocalOp[Raster](r,n) {
  var mean = 0.0
  var `stddev^2` = 0.0

  def createBuilder(r:Raster) = new DoubleRasterBuilder(r.rasterExtent)

  override def init(r:Raster) = {
    val h = FastMapHistogram.fromRaster(r)
    val Statistics(m,_,_,s,_,_) = h.generateStatistics
    mean = m
    `stddev^2` = s*s
  }

  def calc(cursor:DoubleCursor) = {
    var z = 0.0
    var w = 0
    var base = 0.0

    val valueCB = new DoubleFocalCellCB { 
      def apply(x:Int,y:Int,v:Double) = {
        if(x == cursor.focusX && y == cursor.focusY) {
          base = v-mean
        } else {
          z += v-mean
          w += 1
        }
      }
    }

    cursor.allCells.foreach(valueCB)

    (base / `stddev^2` * z) / w
  }
}

// Scalar version:

class NoopResultBuilder extends DoubleResultBuilder[Double] {
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

        val valueCB = new DoubleFocalValueCB { def apply(v:Double) = { z += v ; ws += 1 } }

        cursor.allCells.foreach(valueCB)

        count += base / std2 * z
        ws -= 1 // for focus
        0
    }

    Result(count / ws)
})
