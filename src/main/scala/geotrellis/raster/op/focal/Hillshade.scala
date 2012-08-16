package geotrellis.raster.op.focal

import geotrellis._
import geotrellis._
import geotrellis.process._
import geotrellis.Raster

import scala.math._

object Angles {
  @inline final def radians(d:Double) = d * Pi / 180.0
}
import Angles._

object Hillshade {
  /**
   * Create a default hillshade raster, using a default azimuth and altitude.
   */
  def apply(r:Op[Raster]):Hillshade = Hillshade(r, 315.0, 45.0, 1.0)
}

case class Hillshade(r:Op[Raster], azimuth:Op[Double],
                     altitude:Op[Double], zFactor:Op[Double])
extends Op4(r, azimuth, altitude, zFactor) ({
  (r, az, alt, zf) =>

  // convert angles from degrees (N=0) to to pi radians
  val zenith = radians(90.0 - alt)
  val azimuth = radians(90.0 - az)

  // grab some raster attributes
  val re = r.rasterExtent
  val cw = re.cellwidth
  val ch = re.cellheight

  val strategy = new HillshadeStrategy(re, azimuth, zenith, zf, cw, ch)
  Result(Square(1).handle(r, strategy))
})

class HillshadeStrategy(re:RasterExtent, azimuth:Double, zenith:Double, zFactor:Double, cw:Double, ch:Double)
extends Strategy[Raster, HillshadeCell](Default) {
  val d = IntArrayRasterData.ofDim(re.cols, re.rows)
  def store(col:Int, row:Int, cc:HillshadeCell) = d.set(col, row, cc.calc())
  def get() = Raster(d, re)
  def makeCell() = new HillshadeCell(azimuth, zenith, zFactor, cw, ch)
}

class HillshadeCell(azimuth:Double, zenith:Double, zFactor:Double, cw:Double, ch:Double)
extends Cell[HillshadeCell] {
  var dx = 0 // east - west
  var dy = 0 // south - north
  var baseRow = 0
  var baseCol = 0
  var isUnset = false
  override def center(col:Int, row:Int, r:Raster) {
    baseCol = col
    baseRow = row
    isUnset = r.get(col, row) == NODATA
  }
  def clear() {
    dx = 0
    dy = 0
  }
  def add(cc:HillshadeCell) = sys.error("not supported")
  def remove(cc:HillshadeCell) = sys.error("not supported")
  def remove(col:Int, row:Int, r:Raster) = sys.error("not supported")
  def add(col:Int, row:Int, r:Raster) {
    if (isUnset) return

    // get the cell's value, then weight and apply to dx/dy
    val z = r.get(col, row)
    if (z == NODATA) return

    // these weights will be -1, 0, or 1, and determine how the z value
    // affects the relevant dx/dy. so wx=1, wy=1 would be the SE corner, and
    // wx=0, wy=-1 would be directly N.
    var wx = col - baseCol
    var wy = row - baseRow

    // if not on a diagonal, double the weight for the relevant direction
    if (wx == 0) wy *= 2 else if (wy == 0) wx *= 2

    dx += wx * z
    dy += wy * z
  }

  def calc():Int = {
    if (isUnset) return NODATA

    // scale dx and dy by the total weights involved, times the cell sizes
    val sx = dx / (8 * cw)
    val sy = dy / (8 * ch)

    val slope = atan(zFactor * sqrt(sx * sx + sy * sy))
    val aspect = atan2(sy, -sx) // why are we negating sx again?


    val z = ((cos(zenith) * cos(slope)) + 
             (sin(zenith) * sin(slope) * cos(azimuth - aspect)))

    round(127.0 * max(0.0, z)).toInt
  }
}
