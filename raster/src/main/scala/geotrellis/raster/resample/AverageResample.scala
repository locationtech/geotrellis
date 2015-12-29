package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * Takes the average value for all cells within the index boundaries provided by
  * inheritance from AggregateResample
  */
class AverageResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateAverage(indices: Seq[(Int, Int)]): Double = {
    val (sum, count) =
      indices.foldLeft((0.0, 0)) { case ((sum, count), coords) =>
        val v = tile.getDouble(coords._1, coords._2)
        if (isData(v)) (sum + v, count + 1)
        else (sum, count)
      }
    if (count > 0) (sum / count) else Double.NaN
  }

  def resampleValid(x: Double, y: Double): Int = {
    val doubleAvg = calculateAverage(contributions(x, y))
    if (isData(doubleAvg)) doubleAvg.toInt else Int.MinValue
  }

  def resampleDoubleValid(x: Double, y: Double): Double = {
    calculateAverage(contributions(x, y))
  }

}
