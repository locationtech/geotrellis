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

case class VoxelBounds4D(
  colMin: Int, colMax: Int,
  rowMin: Int, rowMax: Int,
  depthMin: Int, depthMax: Int,
  spissitudeMin: Int, spissitudeMax: Int
) extends VoxelBounds {
  def toVoxelDimensions: VoxelDimensions4D = VoxelDimensions4D(colMax - colMin, rowMax - rowMin, depthMax - depthMin, spissitudeMax - spissitudeMin)
  def split(tb: TilingBounds): Seq[VoxelBounds4D] = split(toVoxelDimensions.withTilingBounds(tb))
  def split(tb: TilingBounds, options: Options): Seq[VoxelBounds4D] = split(toVoxelDimensions.withTilingBounds(tb), options)
  def split(dims: VoxelDimensions4D): Seq[VoxelBounds4D] = split(dims, Options.DEFAULT)
  def split(dims: VoxelDimensions4D, options: Options = Options.DEFAULT): Seq[VoxelBounds4D] = {
    val (tileCols, tileRows, tileDepths, tileSpissitudes) = (dims.width, dims.height, dims.depth, dims.spissitude)
    val (layoutCols, layoutRows, layoutDepths, layoutSpissitudes) = (colMax / dims.width, rowMax / dims.height, depthMax / dims.depth, spissitudeMax / dims.spissitude)

    val splits = Array.ofDim[VoxelBounds4D](layoutCols * layoutRows * layoutDepths * layoutSpissitudes)

    cfor(0)(_ < layoutSpissitudes, _ + 1) { layoutSpissitude =>
      cfor(0)(_ < layoutDepths, _ + 1) { layoutDepth =>
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
            val firstDepth = layoutDepth * tileDepths
            val lastDepth = {
              val x = firstDepth + tileDepths - 1
              if (!options.extend && x > tileDepths - 1) tileDepths - 1
              else x
            }
            val firstSpissitude = layoutSpissitude * tileSpissitudes
            val lastSpissitude = {
              val x = firstSpissitude + tileSpissitudes - 1
              if (!options.extend && x > tileSpissitudes - 1) tileSpissitudes - 1
              else x
            }

            val vb = VoxelBounds4D(firstCol, lastCol, firstRow, lastRow, firstDepth, lastDepth, firstSpissitude, lastSpissitude)
            splits(layoutSpissitude * layoutDepths * layoutCols * layoutRows + layoutDepth * layoutCols * layoutRows + layoutRow * layoutCols + layoutCol) = vb
          }
        }
      }
    }

    splits.toSeq
  }
}
