package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * Uses the following implementation:
  * http://www.paulinternet.nl/?page=bicubic
  *
  * If there is less then 16 points obtainable for the current point the
  * implementation falls back on bilinear interpolation.
  *
  * Might not be very fast since recreating matrices every iteration. I'm
  * guessing its negligible but should be investigated.
  */
class CubicInterpolation(tile: Tile, extent: Extent)
    extends BilinearInterpolation(tile, extent) {

  private def cubicInterpolation(p: Array[Double], x: Double) =
    p(1) + 0.5 * x * (p(2) - p(0) + x * (2.0 * p(0) - 5.0 * p(1) + 4.0 * p(2) - p(3) + x *
      (3.0 * (p(1) - p(2)) + p(3) - p(0))))

  private def biCubicInterpolation(p: Array[Array[Double]], x: Double, y: Double) = {
    val yResults = Array(
      cubicInterpolation(p(0), y),
      cubicInterpolation(p(1), y),
      cubicInterpolation(p(2), y),
      cubicInterpolation(p(3), y)
    )

    cubicInterpolation(yResults, x)
  }

  private def validCubicCoords(leftCol: Int, topRow: Int): Boolean =
    leftCol >= 1 && leftCol < cols - 2 && topRow >= 1 && topRow < rows - 2

  private def getCubicValues(leftCol: Int, topRow: Int, f: (Int, Int) => Double) =
    Array(
      Array(f(leftCol - 1, topRow - 1), f(leftCol, topRow - 1),
        f(leftCol + 1, topRow - 1), f(leftCol + 2, topRow - 1)
      ),
      Array(f(leftCol - 1, topRow), f(leftCol, topRow),
        f(leftCol + 1, topRow), f(leftCol + 2, topRow)
      ),
      Array(f(leftCol - 1, topRow + 1), f(leftCol, topRow + 1),
        f(leftCol + 1, topRow + 1), f(leftCol + 2, topRow + 1)
      ),
      Array(f(leftCol - 1, topRow + 2), f(leftCol, topRow + 2),
        f(leftCol + 1, topRow + 2), f(leftCol + 2, topRow + 2)
      )
    )

  // TODO: talk with Rob and find a way to avoid this code dup.
  override def interpolate(x: Double, y: Double): Int =
    if (!isValid(x, y)) NODATA // does raster have specific NODATA?
    else {
      val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
      if (!validCubicCoords(leftCol, topRow)) bilinearInt(leftCol, topRow, xRatio, yRatio)
      else {
        val cubicMatrix = getCubicValues(leftCol, topRow, tile.get)
        biCubicInterpolation(cubicMatrix, xRatio, yRatio).round.toInt
      }
    }

  override def interpolateDouble(x: Double, y: Double): Double =
    if (!isValid(x, y)) Double.NaN // does raster have specific NODATA?
    else {
      val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
      if (!validCubicCoords(leftCol, topRow)) bilinearDouble(leftCol, topRow, xRatio, yRatio)
      else {
        val cubicMatrix = getCubicValues(leftCol, topRow, tile.getDouble)
        biCubicInterpolation(cubicMatrix, xRatio, yRatio)
      }
    }

}
