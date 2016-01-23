package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import scala.math
import spire.syntax.cfor._

/**
  * Takes the average value for all cells within the index boundaries provided by
  * inheritance from AggregateResample
  */
class MaxResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateMax(indices: Seq[(Int, Int)]): Int =
    indices.foldLeft(Int.MinValue) { case (currentMax, coords) =>
      math.max(currentMax, tile.get(coords._1, coords._2))
    }

  private def calculateMaxDouble(indices: Seq[(Int, Int)]): Double = {
    val doubleMax = indices.foldLeft(Double.MinValue) { case (currentMax, coords) =>
      val v = tile.getDouble(coords._1, coords._2)
      // Double.NaN would *always* be max
      if (isData(v)) math.max(currentMax, v) else currentMax
    }
    if (doubleMax == Double.MinValue) NODATA else doubleMax
  }

  def resampleValid(x: Double, y: Double): Int = calculateMax(contributions(x, y))

  def resampleDoubleValid(x: Double, y: Double): Double = calculateMaxDouble(contributions(x,y))

}
