package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * Implemented exactly as in:
  * http://en.wikipedia.org/wiki/Lanczos_resampling#Multidimensional_interpolation
  *
  * GDAL uses a Lanczos interpolation radius, *a*, as 3, so our cubic size here is
  * 6 * 6.
  *
  * This falls back to the Bilinear interpolation when all 6 * 6 points can't be
  * established.
  */
class LanczosInterpolation(tile: Tile, extent: Extent)
    extends CubicInterpolation(tile, extent, 6) {

  private val lanczos = new LanczosInterpolator

  override def cubicInterpolation(
    p: Array[Array[Double]],
    x: Double,
    y: Double): Double = lanczos.interpolate(p, x, y)

}

class LanczosInterpolator {

  private val Radius = 3 // Taken from GDAL

  private val S = 1 - Radius
  private val E = Radius

  def lanczos(v: Double): Double =
    if (v == 0) 1
    else if (math.abs(v) > 0 && math.abs(v) < Radius)
      (Radius * math.sin(math.Pi * v) *
        math.sin(math.Pi * v / Radius) / math.pow(math.Pi * v, 2))
    else 0

  def interpolate(
    p: Array[Array[Double]],
    x: Double,
    y: Double): Double = {
    var accum = 0.0

    for (i <- S to E; j <- S to E) {
      accum += p(i - S)(j - S) * lanczos(y - j) * lanczos(x - i)
    }

    accum
  }

}
