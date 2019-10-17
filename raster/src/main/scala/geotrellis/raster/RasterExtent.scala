/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import geotrellis.vector.Extent
import scala.math.ceil

import _root_.io.circe.generic.JsonCodec

/**
  * [[GeoAttrsError]] exception.
  */
@JsonCodec
case class GeoAttrsError(msg: String) extends Exception(msg)

/**
  * [[RasterExtent]] objects represent the geographic extent
  * (envelope) of a raster.
  *
  * The Raster extent has two coordinate concepts involved: map
  * coordinates and grid coordinates. Map coordinates are what the
  * Extent class uses, and specifies points using an X coordinate and
  * a Y coordinate. The X coordinate is oriented along west to east
  * such that the larger the X coordinate, the more eastern the
  * point. The Y coordinate is along south to north such that the
  * larger the Y coordinate, the more Northern the point.
  *
  * This contrasts with the grid coordinate system. The grid
  * coordinate system does not actually reference points on the map,
  * but instead a cell of the raster that represents values for some
  * square area of the map. The column axis is similar in that the
  * number gets larger as one goes from west to east; however, the row
  * axis is inverted from map coordinates: as the row number
  * increases, the cell is heading south. The top row is labeled as 0,
  * and the next 1, so that the highest indexed row is the southern
  * most row of the raster.  A cell has a height and a width that is
  * in terms of map units. You can think of it as each cell is itself
  * an extent, with width cellwidth and height cellheight. When a cell
  * needs to be represented or thought of as a point, the center of
  * the cell will be used.  So when gridToMap is called, what is
  * returned is the center point, in map coordinates.
  *
  * Map points are considered to be 'inside' the cell based on these
  * rules:
  *  - If the point is inside the area of the cell, it is included in
  *    the cell.
  *  - If the point lies on the north or west border of the cell, it
  *    is included in the cell.
  *  - If the point lies on the south or east border of the cell, it
  *    is not included in the cell, it is included in the next
  *    southern or eastern cell, respectively.
  *
  * Note that based on these rules, the eastern and southern borders
  * of an Extent are not actually considered to be part of the
  * RasterExtent.
  */

@JsonCodec
case class RasterExtent(
  override val extent: Extent,
  override val cellwidth: Double,
  override val cellheight: Double,
  override val cols: Int,
  override val rows: Int
) extends GridExtent[Int](extent, cellwidth, cellheight, cols, rows) {

  /**
    * Combine two different [[RasterExtent]]s (which must have the
    * same cellsizes).  The result is a new extent at the same
    * resolution.
    */
  def combine(that: RasterExtent): RasterExtent = {
    if (cellwidth != that.cellwidth)
      throw GeoAttrsError(s"illegal cellwidths: $cellwidth and ${that.cellwidth}")
    if (cellheight != that.cellheight)
      throw GeoAttrsError(s"illegal cellheights: $cellheight and ${that.cellheight}")

    val newExtent = extent.combine(that.extent)
    val newRows = ceil(newExtent.height / cellheight).toInt
    val newCols = ceil(newExtent.width / cellwidth).toInt

    new RasterExtent(newExtent, cellwidth, cellheight, newCols, newRows)
  }
  /**
    * Returns a [[RasterExtent]] with the same extent, but a modified
    * number of columns and rows based on the given cell height and
    * width.
    */
  override def withResolution(targetCellWidth: Double, targetCellHeight: Double): RasterExtent = {
    val newCols = math.ceil((extent.xmax - extent.xmin) / targetCellWidth).toInt
    val newRows = math.ceil((extent.ymax - extent.ymin) / targetCellHeight).toInt
    RasterExtent(extent, targetCellWidth, targetCellHeight, newCols, newRows)
  }

  /**
    * Returns a [[RasterExtent]] with the same extent, but a modified
    * number of columns and rows based on the given cell height and
    * width.
    */
  override def withResolution(cellSize: CellSize): RasterExtent =
    withResolution(cellSize.width, cellSize.height)

  /**
   * Returns a [[RasterExtent]] with the same extent and the given
   * number of columns and rows.
   */
  override def withDimensions(targetCols: Int, targetRows: Int): RasterExtent =
    RasterExtent(extent, targetCols, targetRows)


  /**
    * Returns a new [[RasterExtent]] which represents the GridBounds
    * in relation to this RasterExtent.
    */
  def rasterExtentFor(gridBounds: GridBounds[Int]): RasterExtent = {
    val (xminCenter, ymaxCenter) = gridToMap(gridBounds.colMin, gridBounds.rowMin)
    val (xmaxCenter, yminCenter) = gridToMap(gridBounds.colMax, gridBounds.rowMax)
    val (hcw, hch) = (cellwidth / 2, cellheight / 2)
    val e = Extent(xminCenter - hcw, yminCenter - hch, xmaxCenter + hcw, ymaxCenter + hch)
    RasterExtent(e, cellwidth, cellheight, gridBounds.width, gridBounds.height)
  }
}

/**
  * The companion object for the [[RasterExtent]] type.
  */
object RasterExtent {
  /**
    * Create a new [[RasterExtent]] from an Extent, a column, and a
    * row.
    */
  def apply(extent: Extent, cols: Int, rows: Int): RasterExtent = {
    val cw = extent.width / cols
    val ch = extent.height / rows
    RasterExtent(extent, cw, ch, cols, rows)
  }

  /**
    * Create a new [[RasterExtent]] from an Extent and a [[CellSize]].
    */
  def apply(extent: Extent, cellSize: CellSize): RasterExtent = {
    val cols = math.round(extent.width / cellSize.width).toInt
    val rows = math.round(extent.height / cellSize.height).toInt
    RasterExtent(extent, cellSize.width, cellSize.height, cols, rows)
  }

  /**
    * Create a new [[RasterExtent]] from a [[CellGrid]] and an Extent.
    */
  def apply(tile: CellGrid[Int], extent: Extent): RasterExtent =
    apply(extent, tile.cols, tile.rows)

  /**
    * Create a new [[RasterExtent]] from an Extent and a [[CellGrid]].
    */
  def apply(extent: Extent, tile: CellGrid[Int]): RasterExtent =
    apply(extent, tile.cols, tile.rows)
}
