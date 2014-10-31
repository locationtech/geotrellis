package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * Takes the most common value in the tile and interpolates all points to that.
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

  private def calculateMostCommonValue(nodata: Double, f: (Int, Int) => Double) = {
    ???
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
