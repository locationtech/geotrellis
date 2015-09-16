package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * This abstract class serves as a base class for the family of
  * cubic resample algorithms implemented. As a constructor argument
  * it takes the dimension of the cube. It takes the closest dimension ^ 2
  * points and then resamples over those points.
  *
  * If there is less then dimension ^ 2 points obtainable for the current point
  * the implementation falls back on bilinear resample.
  *
  * Note that this class is single-threaded.
  */
abstract class CubicResample(tile: Tile, extent: Extent, dimension: Int)
    extends BilinearResample(tile, extent) {

  private val cubicTile =
    ArrayTile(Array.ofDim[Double](dimension * dimension), dimension, dimension)

  protected def cubicResample(
    t: Tile,
    x: Double,
    y: Double): Double

  private def validCubicCoords(leftCol: Int, topRow: Int): Boolean = {
    val offset = dimension / 2
    val low = offset - 1
    leftCol >= low && leftCol < cols - offset && topRow >= low && topRow < rows - offset
  }

  private def setCubicValues(leftCol: Int, topRow: Int, f: (Int, Int) => Double) = {
    val offset = dimension / 2

    cfor(0)(_ < dimension, _ + 1) { i =>
      cfor(0)(_ < dimension, _ + 1) { j =>
        val v = f(leftCol - offset + 1 + j, topRow - offset + 1 + i)
        cubicTile.setDouble(j, i, v)
      }
    }
  }

  override def resampleValid(x: Double, y: Double): Int = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    if (!validCubicCoords(leftCol, topRow)) {
      bilinearInt(leftCol, topRow, xRatio, yRatio)
    } else {
      setCubicValues(leftCol, topRow, tile.get)
      cubicResample(cubicTile, xRatio, yRatio).round.toInt
    }
  }

  override def resampleDoubleValid(x: Double, y: Double): Double = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    if (!validCubicCoords(leftCol, topRow)) {
      bilinearDouble(leftCol, topRow, xRatio, yRatio)
    } else {
      setCubicValues(leftCol, topRow, tile.getDouble)
      cubicResample(cubicTile, xRatio, yRatio)
    }
  }

}
