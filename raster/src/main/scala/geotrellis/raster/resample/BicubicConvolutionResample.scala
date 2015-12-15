package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.apache.commons.math3.analysis.interpolation._

/**
  * Uses the following implementation:
  * http://www.paulinternet.nl/?page=bicubic
  */
class BicubicConvolutionResample(tile: Tile, extent: Extent)
    extends BicubicResample(tile, extent, 4) {

  private val resampler = new CubicConvolutionResample

  override def uniCubicResample(p: Array[Double], x: Double) =
    resampler.resample(p, x)
}

class CubicConvolutionResample {

  def resample(p: Array[Double], x: Double) =
    p(1) + 0.5 * x * (p(2) - p(0) + x * (2.0 * p(0) - 5.0 * p(1) + 4.0 * p(2) - p(3) + x *
      (3.0 * (p(1) - p(2)) + p(3) - p(0))))

}
