package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * This abstract class serves as a base class for the family of
  * cubic interpolation algorithms implemented. As a constructor argument
  * it takes the dimension of the cube. It takes the closest dimension ^ 2
  * points and then interpolates over those points.
  *
  * If there is less then dimension ^ 2 points obtainable for the current point
  * the implementation falls back on bilinear interpolation.
  */
abstract class CubicInterpolation(tile: Tile, extent: Extent, dimension: Int)
    extends BilinearInterpolation(tile, extent) {

  protected def cubicInterpolation(
    p: Array[Array[Double]],
    x: Double,
    y: Double): Double

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

  // TODO: talk with Rob and find a way to avoid this code dup.
  override def interpolateValid(x: Double, y: Double): Int = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    if (!validCubicCoords(leftCol, topRow)) bilinearInt(leftCol, topRow, xRatio, yRatio)
    else {
      val cubicMatrix = getCubicValues(leftCol, topRow, tile.get)
      cubicInterpolation(cubicMatrix, xRatio, yRatio).round.toInt
    }
  }

  override def interpolateDoubleValid(x: Double, y: Double): Double = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    if (!validCubicCoords(leftCol, topRow)) bilinearDouble(leftCol, topRow, xRatio, yRatio)
    else {
      val cubicMatrix = getCubicValues(leftCol, topRow, tile.getDouble)
      cubicInterpolation(cubicMatrix, xRatio, yRatio)
    }
  }

}
