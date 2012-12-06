package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.Raster

import scala.math._

import Angles._

object Hillshade {
  /**
   * Create a default hillshade raster, using a default azimuth and altitude.
   */
  def apply(r:Op[Raster]):Hillshade = Hillshade(r, 315.0, 45.0, 1.0)
}

case class Hillshade(r:Op[Raster], azimuthOp:Op[Double], altitudeOp:Op[Double], zFactorOp:Op[Double])
    extends FocalOp3[Double,Double,Double,Raster](r,Square(1),azimuthOp,altitudeOp,zFactorOp)({
  (r,n) => new CursorCalculation with ShortRasterDataResult with Initialization3[Double,Double,Double] {
    var azimuth = 0.0
    var zenith = 0.0
    var zFactor = 0.0
    var cellWidth = 0.0
    var cellHeight = 0.0

    def init(r:Raster,az:Double,al:Double,z:Double) = {
      super.init(r)

      azimuth = radians(90.0 - az)
      zenith = radians(90.0 - al)
      zFactor = z
      cellWidth = r.rasterExtent.cellwidth
      cellHeight = r.rasterExtent.cellheight
    }

    def calc(r:Raster,cursor:Cursor) = {
      val (slope,aspect) = SlopeAspectCalculator.getSlopeAndAspect(r,cursor,zFactor,cellWidth,cellHeight)
      val z = ((cos(zenith) * cos(slope)) +
               (sin(zenith) * sin(slope) * cos(azimuth - aspect)))
        data.set(cursor.focusX,cursor.focusY, round(127.0 * max(0.0, z)).toInt)
    }
  }
})
