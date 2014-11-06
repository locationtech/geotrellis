package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * All classes inheriting from this class uses the interpolation as follows:
  * First the 4 rows each containing 4 points are
  * interpolated, then each result is stored and together interpolated.
  *
  * If there can't be 16 points resolved, it falls back to bilinear
  * interpolation.
  */
abstract class BicubicInterpolation(tile: Tile, extent: Extent, dimension: Int)
    extends CubicInterpolation(tile, extent, dimension) {

  private val columnResults = Array.ofDim[Double](dimension)

  private val iterArray = Array.ofDim[Double](dimension)

  protected def uniCubicInterpolation(p: Array[Double], v: Double): Double

  override def cubicInterpolation(
    t: Tile,
    x: Double,
    y: Double): Double = {

    cfor(0)(_ < dimension, _ + 1) { i =>
      cfor(0)(_ < dimension, _ + 1) { j =>
        iterArray(j) = t.getDouble(j, i)
      }

      columnResults(i) = uniCubicInterpolation(iterArray, x)
    }

    uniCubicInterpolation(columnResults, y)
  }

}
