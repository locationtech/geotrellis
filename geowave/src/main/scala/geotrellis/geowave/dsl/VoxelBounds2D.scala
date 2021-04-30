/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.dsl

import geotrellis.raster.split.Split.Options
import spire.syntax.cfor.cfor

case class VoxelBounds2D(colMin: Int, colMax: Int, rowMin: Int, rowMax: Int) extends VoxelBounds {
  def toVoxelDimensions: VoxelDimensions2D = VoxelDimensions2D(colMax - colMin, rowMax - rowMin)

  def split(tb: TilingBounds): Seq[VoxelBounds2D] = split(toVoxelDimensions.withTilingBounds(tb))

  def split(tb: TilingBounds, options: Options): Seq[VoxelBounds2D] = split(toVoxelDimensions.withTilingBounds(tb), options)

  def split(dims: VoxelDimensions2D): Seq[VoxelBounds2D] = split(dims, Options.DEFAULT)

  def split(dims: VoxelDimensions2D, options: Options): Seq[VoxelBounds2D] = {
    val (tileCols, tileRows) = dims.width -> dims.height
    val (layoutCols, layoutRows) = (colMax / dims.width, rowMax / dims.height)

    val splits = Array.ofDim[VoxelBounds2D](layoutCols * layoutRows)

    cfor(0)(_ < layoutRows, _ + 1) { layoutRow =>
      cfor(0)(_ < layoutCols, _ + 1) { layoutCol =>
        val firstCol = layoutCol * tileCols
        val lastCol = {
          val x = firstCol + tileCols - 1
          if (!options.extend && x > tileCols - 1) tileCols - 1
          else x
        }
        val firstRow = layoutRow * tileRows
        val lastRow = {
          val x = firstRow + tileRows - 1
          if (!options.extend && x > tileRows - 1) tileRows - 1
          else x
        }

        val vb = VoxelBounds2D(firstCol, lastCol, firstRow, lastRow)
        splits(layoutRow * layoutCols + layoutCol) = vb
      }
    }

    splits.toSeq
  }
}