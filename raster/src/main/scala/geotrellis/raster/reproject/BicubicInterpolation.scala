package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * This abstract class serves as a base class for the family of
  * bicubic interpolation algorithms implemented. As a constructor argument
  * it takes the dimension of the cube. It takes the closest dimension ^ 2
  * points and then interpolates over those points.
  *
  * If there is less then dimension ^ 2 points obtainable for the current point
  * the implementation falls back on bilinear interpolation.
  *
  * All classes inheriting from this class uses the interpolation as follows:
  * First the *dimension* rows each containing four points are interpolated,
  * then each result is stored and together interpolated.
  */
abstract class BicubicInterpolation(tile: Tile, extent: Extent, dimension: Int)
    extends BilinearInterpolation(tile, extent) {

  protected def uniCubicInterpolation(p: Array[Double], v: Double): Double

  private def biCubicInterpolation(
    p: Array[Array[Double]],
    x: Double,
    y: Double): Double = {
    val columnResults = Array.ofDim[Double](dimension)
    for (i <- 0 until dimension) columnResults(i) = uniCubicInterpolation(p(i), x)
    uniCubicInterpolation(columnResults, y)
  }

  private def validCubicCoords(leftCol: Int, topRow: Int): Boolean = {
    val offset = dimension / 2
    val low = offset - 1
    leftCol >= low && leftCol < cols - offset && topRow >= low && topRow < rows - offset
  }

  private def getCubicValues(leftCol: Int, topRow: Int, f: (Int, Int) => Double) = {
    val offset = dimension / 2
    val res = Array.ofDim[Array[Double]](dimension)
    for (i <- 0 until dimension) {
      res(i) = Array.ofDim[Double](dimension)
      for (j <- 0 until dimension) {
        res(i)(j) = f(leftCol - offset + 1 + j, topRow - offset + 1 + i)
      }
    }

    res
  }

  def derp(l: Int, t: Int) = println(validCubicCoords(l, t))

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
