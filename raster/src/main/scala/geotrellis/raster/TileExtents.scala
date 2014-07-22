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
import geotrellis.vector.Extent

case class TileExtents(extent: Extent, tileLayout: TileLayout) {
  val cellSize = tileLayout.cellSize(extent)

  def apply(tileCol: Int, tileRow: Int): Extent = {
    Extent(
      xCoord(tileCol),
      yCoord(tileRow + 1), 
      xCoord(tileCol + 1), 
      yCoord(tileRow)
    )
  }

  def apply(tileIndex: Int): Extent = {
    val row = tileIndex / tileLayout.tileCols
    val col = tileIndex - (row * tileLayout.tileCols)
    apply(col, row)
  }

  /**
   * Given an extent and resolution (RasterExtent), return the geographic
   * X-coordinates for each tile boundary in this raster data. For example,
   * if we have a 2x2 ArrayTile, with a raster extent whose X coordinates
   * span 13.0 - 83.0 (i.e. cellwidth is 35.0), we would return the following
   * for the corresponding input:
   *
   *  Input     Output
   * -------   -------- 
   *    0        13.0
   *    1        48.0
   *    2        83.0
   *
   */
  private def xCoord(col: Int): Double =
    extent.xmin + (col * cellSize.width * tileLayout.pixelCols)

  /**
   * This method is identical to getXCoord except that it functions on the
   * Y-axis instead.
   * 
   * Note that the origin tile (0, 0) is in the upper left of the extent, so the
   * upper left corner of the origin tile is (xmin, ymax).
   */
  private def yCoord(row: Int): Double =
    extent.ymax - (row * cellSize.height * tileLayout.pixelRows)
}
