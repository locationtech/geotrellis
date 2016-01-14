package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import scala.math
import spire.syntax.cfor._

/**
  * Takes the average value for all cells within the index boundaries provided by
  * inheritance from AggregateResample
  */
class MinResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateMin(indices: Seq[(Int, Int)]): Int =
    indices.foldLeft(Int.MaxValue) { case (currentMin, coords) =>
      math.min(currentMin, tile.get(coords._1, coords._2))
    }

  private def calculateMinDouble(indices: Seq[(Int, Int)]): Double =
    indices.foldLeft(Double.MaxValue) { case (currentMin, coords) =>
      math.min(currentMin, tile.getDouble(coords._1, coords._2))
    }

  def resampleValid(x: Double, y: Double): Int = calculateMin(contributions(x, y))

  def resampleDoubleValid(x: Double, y: Double): Double = calculateMinDouble(contributions(x,y))

}
