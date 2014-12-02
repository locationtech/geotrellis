package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import org.apache.commons.math3.analysis.interpolation._

/**
  * Uses the following implementation:
  * http://www.paulinternet.nl/?page=bicubic
  */
class BicubicConvolutionInterpolation(tile: Tile, extent: Extent)
    extends BicubicInterpolation(tile, extent, 4) {

  private val interpolator = new CubicConvolutionInterpolation

  override def uniCubicInterpolation(p: Array[Double], x: Double) =
    interpolator.interpolate(p, x)

}

class CubicConvolutionInterpolation {

  def interpolate(p: Array[Double], x: Double) =
    p(1) + 0.5 * x * (p(2) - p(0) + x * (2.0 * p(0) - 5.0 * p(1) + 4.0 * p(2) - p(3) + x *
      (3.0 * (p(1) - p(2)) + p(3) - p(0))))

}
