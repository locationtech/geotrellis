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
extends Strategy[Raster, HillshadeCell](Sliding) {
  val d = IntArrayRasterData.ofDim(re.cols, re.rows)
  def store(col:Int, row:Int, cc:HillshadeCell) = d.set(col, row, cc.calc())
  def get() = Raster(d, re)
  def makeCell() = new HillshadeCell(azimuth, zenith, zFactor, cw, ch)
}

class HillshadeCell(azimuth:Double, zenith:Double, zFactor:Double, cw:Double, ch:Double)
extends Cell[HillshadeCell] {
  var west = new Array[Int](3)
  var base = new Array[Int](3)
  var east = new Array[Int](3)

  var northRow = 0

  override def center(col:Int, row:Int, r:Raster) {
    northRow = row - 1

    val tmp = west
    west = base
    base = east
    east = tmp
  }
  def clear() {
    west = new Array[Int](3)
    base = new Array[Int](3)
    east = new Array[Int](3)
  }

  def add(cc:HillshadeCell) = sys.error("not supported")
  def remove(cc:HillshadeCell) = sys.error("not supported")
  def remove(col:Int, row:Int, r:Raster) {}
  def add(col:Int, row:Int, r:Raster) {
    east(row - northRow) = r.get(col, row)
  }

  def calc():Int = {
    if (base(1) == NODATA) return NODATA

    // east - west
    val sx = (east(0) + 2*east(1) + east(2) - west(0) - 2*west(1) - west(2)) / (8 * cw)

    // south - north
    val sy = (west(2) + 2*base(2) + east(2) - west(0) - 2*base(0) - east(0)) / (8 * ch)

    val slope = atan(zFactor * sqrt(sx * sx + sy * sy))
    val aspect = atan2(sy, -sx) // why are we negating sx again?

    val z = ((cos(zenith) * cos(slope)) + 
             (sin(zenith) * sin(slope) * cos(azimuth - aspect)))

    round(127.0 * max(0.0, z)).toInt
  }
}
