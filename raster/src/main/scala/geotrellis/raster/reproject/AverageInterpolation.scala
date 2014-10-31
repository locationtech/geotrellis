package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * Takes the average value of the tile and interpolates all points to that.
  */
class AverageInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  private val cols = tile.cols
  private val rows = tile.rows

  private val westBound = extent.xmin // TODO: duplication
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  private val RoundingScale = 5

  private lazy val average = calculateAverage(NODATA, tile.get).round.toInt

  private lazy val averageDouble = calculateAverage(Double.NaN, tile.getDouble)

  /**
    * Calculates the mean/average of the tile.
    * Uses this algorithm to avoid overflow:
    * http://www.heikohoffmann.de/htmlthesis/node134.html
    *
    */
  private def calculateAverage(nodata: Double, f: (Int, Int) => Double) = {
    var avg = 0.0
    var t = 1

    cfor(0)(_ < cols, _ + 1) { i =>
      cfor(0)(_ < rows, _ + 1) { j =>
        val c = f(i, j)
        if (nodata != c && !c.isNaN) {
          avg += (c - avg) / t
          t += 1
        }
      }
    }

    if (t != 1)
      BigDecimal(avg)
        .setScale(RoundingScale, BigDecimal.RoundingMode.HALF_UP)
        .toDouble
    else
      nodata
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
