/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import geotrellis._
import geotrellis.feature.Extent

object TileLayout {
  def singleTile(cols: Int, rows: Int) =
    TileLayout(1, 1, cols, rows)

  def singleTile(re: RasterExtent) =
    TileLayout(1, 1, re.cols, re.rows)
}

/**
 * This class stores the layout of a tiled raster: the number of tiles (in
 * cols/rows) and also the size of each tile (in cols/rows of pixels).
 */
case class TileLayout(tileCols: Int, tileRows: Int, pixelCols: Int, pixelRows: Int) {
  def isTiled = tileCols > 1 || tileRows > 1

  /**
   * Return the total number of columns across all the tiles.
   */
  def totalCols: Long = tileCols.toLong * pixelCols

  /**
   * Return the total number of rows across all the tiles.
   */
  def totalRows: Long = tileRows.toLong * pixelRows

  /** Gets the index of a tile at col, row. */
  def getTileIndex(col: Long, row: Long) =
    row * tileCols + col

  def getTileXY(index: Long) =
    (index % tileCols, index / tileCols)
    
  def cellSize(extent: Extent): CellSize = 
    CellSize(extent.width / totalCols, extent.height / totalRows)
}
