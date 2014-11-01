package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import collection._

import spire.syntax.cfor._

/**
  * Takes the most common value in the tile and interpolates all points to that.
  */
class ModeInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  private val cols = tile.cols
  private val rows = tile.rows

  private val westBound = extent.xmin // TODO: duplication
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  private lazy val average =
    calculateMostCommonValue(NODATA, tile.getDouble).toInt

  private lazy val averageDouble =
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

  // TODO: duplication
  protected def isValid(x: Double, y: Double) =
    x >= westBound && x <= eastBound && y >= southBound && y <= northBound

  def interpolate(x: Double, y: Double): Int =
    if (!isValid(x, y)) NODATA
    else average

  def interpolateDouble(x: Double, y: Double): Double =
    if (!isValid(x, y)) Double.NaN
    else averageDouble

}
