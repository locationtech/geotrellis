package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * All classes inheriting from this class uses the interpolation as follows:
  * First the *dimension* rows each containing *dimension* points are
  * interpolated, then each result is stored and together interpolated.
  *
  * If there can't be dimension ^ 2 points resolved, it falls back to bilinear
  * interpolation.
  */
abstract class BicubicInterpolation(tile: Tile, extent: Extent, dimension: Int)
    extends CubicInterpolation(tile, extent, dimension) {

  protected def uniCubicInterpolation(p: Array[Double], v: Double): Double

  override def cubicInterpolation(
    p: Array[Array[Double]],
    x: Double,
    y: Double): Double = {
    val columnResults = Array.ofDim[Double](dimension)
    for (i <- 0 until dimension) columnResults(i) = uniCubicInterpolation(p(i), x)
    uniCubicInterpolation(columnResults, y)
  }

}
