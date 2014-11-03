package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import org.apache.commons.math3.analysis.function.Sin

import spire.syntax.cfor._

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

  private val sin = new Sin

  private val PiDivRadius = math.Pi / Radius

  private val Pi2 = math.Pi * math.Pi

  @inline
  def lanczos(v: Double): Double =
    if (v == 0) 1
    else if (v < Radius && v > -Radius)
      (Radius * sin.value(math.Pi * v) * sin.value(v * PiDivRadius) /
        (Pi2 * v * v))
    else 0

  @inline
  def interpolate(
    p: Array[Array[Double]],
    x: Double,
    y: Double): Double = {
    var accum = 0.0

    cfor(S)(_ <= E, _ + 1) { i =>
      cfor(S)(_ <= E, _ + 1) { j =>
        accum += p(i - S)(j - S) * lanczos(y - j) * lanczos(x - i)
      }
    }

    accum
  }

}
