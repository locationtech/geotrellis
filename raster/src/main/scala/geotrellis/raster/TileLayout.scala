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


/**
  * The companion object associated with the [[TileLayout]] type.
  */
object TileLayout {

  /**
    * Produce a new [[TileLayout]] with the given number of columns
    * and rows.
    */
  def singleTile(cols: Int, rows: Int) =
    TileLayout(1, 1, cols, rows)

  /**
    * Produce a new [[TileLayout]] dimensions determined from the
    * given [[RasterExtent]].
    */
  def singleTile(re: RasterExtent) =
    TileLayout(1, 1, re.cols, re.rows)
}

/**
 * @param layoutCols number of columns in the layout, East to West
 * @param layoutRows number of rows in the layout grid, North to South
 * @param tileCols   number of pixel columns in each tile, East to West
 * @param tileRows   number of pixel rows in each tile, North to South
 */
case class TileLayout(layoutCols: Int, layoutRows: Int, tileCols: Int, tileRows: Int) {

  require(
    layoutCols > 0 && layoutRows > 0 && tileCols > 0 && tileRows > 0,
    s"TileLayout should contain only cols and rows number > 0: $toString"
  )

  def isTiled = layoutCols > 1 || layoutRows > 1

  /**
   * Return the total number of columns across all the tiles.
   */
  def totalCols: Long = layoutCols.toLong * tileCols

  /**
   * Return the total number of rows across all the tiles.
   */
  def totalRows: Long = layoutRows.toLong * tileRows

  def layoutDimensions: (Int, Int) = (layoutCols, layoutRows)
  def tileDimensions: (Int, Int) = (tileCols, tileRows)

  def tileSize: Int = tileCols * tileRows

  /**
    * Compute the size of the cells, the [[CellSize]], in the given
    * Extent.
    */
  def cellSize(extent: Extent): CellSize =
    CellSize(extent.width / totalCols, extent.height / totalRows)

  /**
    * Combine the present [[TileLayout]] with another one and return
    * then result.
    */
  def combine(other: TileLayout) = {
    val maxLayoutCols = if(layoutCols > other.layoutCols) layoutCols else other.layoutCols
    val maxLayoutRows = if(layoutRows > other.layoutRows) layoutRows else other.layoutRows
    val maxTileCols   = if(tileCols > other.tileCols) tileCols else other.tileCols
    val maxTileRows   = if(tileRows > other.tileRows) tileRows else other.tileRows

    TileLayout(maxLayoutCols, maxLayoutRows, maxTileCols, maxTileRows)
  }
}
