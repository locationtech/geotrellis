package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * All classes inheriting from this class uses the resample as follows:
  * First the 4 rows each containing 4 points are
  * resampled, then each result is stored and together resampled.
  *
  * If there can't be 16 points resolved, it falls back to bilinear
  * resampling.
  */
abstract class BicubicResample(tile: Tile, extent: Extent, dimension: Int)
    extends CubicResample(tile, extent, dimension) {

  private val columnResults = Array.ofDim[Double](dimension)

  private val iterArray = Array.ofDim[Double](dimension)

  protected def uniCubicResample(p: Array[Double], v: Double): Double

  override def cubicResample(
    t: Tile,
    x: Double,
    y: Double): Double = {

    cfor(0)(_ < dimension, _ + 1) { i =>
      cfor(0)(_ < dimension, _ + 1) { j =>
        iterArray(j) = t.getDouble(j, i)
      }

      columnResults(i) = uniCubicResample(iterArray, x)
    }

    uniCubicResample(columnResults, y)
  }

}
