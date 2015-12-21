package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import scala.math.{min, max}

abstract class AggregateResample(tile: Tile, extent: Extent, targetCS: CellSize) extends Resample(tile, extent) {

  val srcCellWidth = extent.width / tile.cols
  val srcCellHeight = extent.height / tile.rows
  val halfSrcCellWidth = srcCellWidth / 2
  val halfSrcCellHeight = srcCellHeight / 2

  val halfTargetWidth = targetCS.width / 2
  val halfTargetHeight = targetCS.height / 2

  def xIndices(x: Double): (Int, Int) = {
    // Distance from the left of tile
    val dLeftX = x - extent.xmin - halfSrcCellWidth

    // calc cell distance from left edge
    val dLeftCellLeft: Double = dLeftX - halfTargetWidth
    val dLeftCellRight: Double = dLeftX + halfTargetWidth

    // Calculate indices
    val leftIndex =
      if (dLeftCellLeft > 0) (dLeftCellLeft / srcCellWidth).ceil.toInt
      else 0
    val rightIndex =
      if(dLeftCellRight < extent.width) (dLeftCellRight / srcCellWidth).floor.toInt
      else tile.cols - 1

    (leftIndex, rightIndex)
  }
  def yIndices(y: Double): (Int, Int) = {
    // Distance from top of tile
    val dTopY = extent.ymax - y - halfSrcCellHeight

    // calc cell distance from top
    val dTopCellTop: Double = dTopY - halfTargetHeight
    val dTopCellBottom: Double = dTopY + halfTargetHeight

    // Calculate indices
    val topIndex =
      if (dTopCellTop > 0) (dTopCellTop / srcCellHeight).ceil.toInt
      else 0
    val bottomIndex =
      if (dTopCellBottom < extent.height) (dTopCellBottom / srcCellHeight).floor.toInt
      else tile.rows - 1

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
