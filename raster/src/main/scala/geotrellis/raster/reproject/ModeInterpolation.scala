package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import collection._

import spire.syntax.cfor._

/**
  * Takes the most common value in the tile and interpolates all points to that.
  */
class ModeInterpolation(tile: Tile, extent: Extent)
    extends Interpolation(tile, extent) {

  private lazy val mostCommonValue =
    calculateMostCommonValue(NODATA, tile.getDouble).toInt

  private lazy val mostCommonValueDouble =
    calculateMostCommonValue(Double.NaN, tile.getDouble)

  private def calculateMostCommonValue(nodata: Double, f: (Int, Int) => Double) = {
    val map = mutable.HashMap[Double, Int]()
    var max = nodata
    var maxAccum = 0

    cfor(0)(_ < cols, _ + 1) { i =>
      cfor(0)(_ < rows, _ + 1) { j =>
        val c = f(i, j)
        if (c != nodata && !c.isNaN) {
          val accum = map.getOrElse(c, 0) + 1
          map += (c -> accum)

          if (accum > maxAccum) {
            max = c
            maxAccum = accum
          }
        }
      }
    }

    max
  }

  override def interpolateValid(x: Double, y: Double): Int =
    mostCommonValue

  override def interpolateDoubleValid(x: Double, y: Double): Double =
    mostCommonValueDouble

}
