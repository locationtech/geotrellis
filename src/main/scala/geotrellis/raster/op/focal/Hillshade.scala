package geotrellis.raster.op.focal

import geotrellis._
import geotrellis._
import geotrellis.process._
import geotrellis.Raster

import scala.math._

object Angles {
  @inline final def radians(d:Double) = d * Pi / 180.0
  @inline final def degrees(r:Double) = r * 180.0 / Pi
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
  val cw = r.rasterExtent.cellwidth
  val ch = r.rasterExtent.cellheight

  val dfn = HillshadeFocalOpDef(azimuth, zenith, zf, cw, ch)
  FocalOp.getResult(r, Sliding, Square(1), dfn)
})

protected[focal] case class HillshadeFocalOpDef(azimuth:Double, 
						zenith:Double, 
						zFactor:Double, 
						cw:Double, 
						ch:Double) extends IntFocalOpDefinition {
  def newCalc = new HillshadeCalc(azimuth,zenith,zFactor,cw,ch)
  override def newData(r: Raster) = ShortFocalOpData(r.rasterExtent)
}

protected[focal] case class HillshadeCalc(azimuth:Double, 
					  zenith:Double,
					  zFactor:Double, 
					  cw:Double, 
					  ch:Double) extends SlopeAspectCalculator[Int](zFactor,cw,ch) {
  def getResult:Int = {
    if (base(1) == NODATA) return NODATA

    val (slope,aspect) = getSlopeAndAspect
      
    val z = ((cos(zenith) * cos(slope)) + 
             (sin(zenith) * sin(slope) * cos(azimuth - aspect)))

    round(127.0 * max(0.0, z)).toInt
  }
}
