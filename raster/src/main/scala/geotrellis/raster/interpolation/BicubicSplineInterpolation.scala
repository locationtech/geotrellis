package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

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

  private val interpolator = new CubicSplineInterpolation

  override def uniCubicInterpolation(p: Array[Double], x: Double) =
    interpolator.interpolate(p, x)

}

class CubicSplineInterpolation {

  private val Xs = Array(0, 1, 2, 3, 4, 5)

  def interpolate(p: Array[Double], x: Double): Double = {
    val n = p.size
    val u = Array.ofDim[Double](n + 3)
    cfor(0)(_ < n - 1, _ + 1) { i =>
      u(i + 2) = (p(i + 1) - p(i)) / (Xs(i + 1) - Xs(i))
    }

    u(n + 1) = 2 * u(n) - u(n - 1)
    u(n + 2) = 2 * u(n + 1) - u(n)
    u(1) = 2 * u(2) - u(3)
    u(0) = 2 * u(1) - u(2)

    val yp = Array.ofDim[Double](n)

    cfor(0)(_ < n, _ + 1) { i =>
      val a = math.abs(u(i + 3) - u(i + 2))
      val b = math.abs(u(i + 1) - u(i))

      yp(i) = if (a + b != 0) (a * u(i + 1) + b * u(i + 2)) / (a + b)
      else (u(i + 2) + u(i + 1)) / 2
    }

    p(2) + yp(2) * x + (3 * u(4) - 2 * yp(2) - yp(3)) * x * x + (yp(2) + yp(3) -
      2 * u(4)) * x * x * x
  }

}
