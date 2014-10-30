package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * Uses the Akima cubic spline interpolation.
  *
  * The paper can be found here:
  * http://www.leg.ufpr.br/lib/exe/fetch.php/wiki:internas:biblioteca:akima.pdf
  *
  * A more graspable explanation can be found here:
  * http://www.iue.tuwien.ac.at/phd/rottinger/node60.html
  */
class BicubicSplineInterpolation(tile: Tile, extent: Extent)
    extends BicubicInterpolation(tile, extent, 6) {

  private val interpolator = new CubicSplineInterpolation()

  override def uniCubicInterpolation(p: Array[Double], x: Double) =
    interpolator.interpolate(p, x)

}

class CubicSplineInterpolation {

  def interpolate(p: Array[Double], x: Double): Double = ???

}
