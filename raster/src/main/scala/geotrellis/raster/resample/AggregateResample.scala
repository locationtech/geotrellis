package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import scala.math.{min, max}

abstract class AggregateResample(tile: Tile, extent: Extent, targetCS: CellSize) extends Resample(tile, extent) {

  val srcCellWidth = extent.width / tile.cols
  val srcCellHeight = extent.height / tile.rows

  val halfWidth = targetCS.width / 2
  val halfHeight = targetCS.height / 2

  def xIndices(x: Double): (Int, Int) = {
    val leftbound: Double = max(x - halfWidth, extent.xmin)
    val rightbound: Double = min(x + halfWidth, extent.xmax - 0.0001)

    val leftIndex = min(((leftbound - extent.xmin) / srcCellWidth).ceil.toInt, tile.cols - 1)
    val rightIndex = ((rightbound - extent.xmin) / srcCellWidth).floor.toInt
    (leftIndex, rightIndex)
  }
  def yIndices(y: Double): (Int, Int) = {
    // The Y Coordinate needs to be inverted
    val invY = extent.ymax - y
    val top: Double = max(invY - halfWidth, extent.ymin)
    val bottom: Double = min(invY + halfWidth, extent.ymax - 0.0001)

    val topIndex = min(((top - extent.ymin) / srcCellHeight).ceil.toInt, tile.rows - 1)
    val bottomIndex = ((bottom - extent.ymin) / srcCellHeight).floor.toInt
    (topIndex, bottomIndex)
  }

  def contributions(x: Double, y: Double): Seq[(Int, Int)] = {
    val (xLow, xHigh) = xIndices(x)
    val (yLow, yHigh) = yIndices(y)

    if (xLow > xHigh || yLow > yHigh) Vector[(Int, Int)]()
    else for {
      xs <- xLow to xHigh
      ys <- yLow to yHigh
    } yield (xs, ys)
  }
}
