package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * Takes the average value of the tile and resamples all points to that.
  */
class AverageResample(tile: Tile, extent: Extent)
    extends Resample(tile, extent) {

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

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val c = f(col, row)
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

  override def resampleValid(x: Double, y: Double): Int = average

  override def resampleDoubleValid(x: Double, y: Double): Double = averageDouble

}
