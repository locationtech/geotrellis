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

package geotrellis.raster.split

import geotrellis.raster._

import spire.syntax.cfor._

import Split.Options

trait RasterExtentSplitMethods extends SplitMethods[RasterExtent] {
  def split(tileLayout: TileLayout, options: Options): Array[RasterExtent] = {
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    val splits = Array.ofDim[RasterExtent](tileLayout.layoutCols * tileLayout.layoutRows)
    cfor(0)(_ < tileLayout.layoutRows, _ + 1) { layoutRow =>
      cfor(0)(_ < tileLayout.layoutCols, _ + 1) { layoutCol =>
        val firstCol = layoutCol * tileCols
        val lastCol = {
          val x = firstCol + tileCols - 1
          if(!options.extend && x > self.cols - 1) self.cols - 1
          else x
        }
        val firstRow = layoutRow * tileRows
        val lastRow = {
          val x = firstRow + tileRows - 1
          if(!options.extend && x > self.rows - 1) self.rows - 1
          else x
        }
        val gb = GridBounds(firstCol, firstRow, lastCol, lastRow)
        splits(layoutRow * tileLayout.layoutCols + layoutCol) =
          self.rasterExtentFor(gb)
      }
    }

    splits
  }
}
