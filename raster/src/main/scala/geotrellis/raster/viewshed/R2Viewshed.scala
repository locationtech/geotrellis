/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector._

import java.util.Arrays.binarySearch
import java.util.Comparator


object R2Viewshed extends Serializable {

  type Ray = (Double, Double) // slope, alpha

  class RayComparator extends Comparator[Ray] {
    def compare(left: Ray, right: Ray): Int =
      if (left._1 < right._1) -1
      else if (left._1 > right._1) +1
      else 0
  }

  val rayComparator = new RayComparator

  def apply(tile: Tile, col: Int, row: Int): Tile =
    R2Viewshed.apply(tile, col, row, 0)

  def apply(
    tile: Tile,
    startCol: Int, startRow: Int, height: Double,
    resolution: Double = 1.0,
    rays: Array[Ray] = null
  ): Tile = {
    val cols = tile.cols
    val rows = tile.rows
    val re = RasterExtent(Extent(0, 0, cols, rows), cols, rows)
    val viewshed = ArrayTile.empty(IntCellType, cols, rows)

    val viewHeight =
      if (height >= 0)
        tile.getDouble(startCol, startRow) + height
      else
        -height
    var slope: Double = 0.0
    var alpha: Double = 0.0

    @inline def slopeToAlpha(theta: Double): Double = {
      val index = binarySearch(rays, (theta, Double.NaN), rayComparator)
      if (index >= 0) rays(index)._2
      else {
        val place = -1 - index
        if (place == rays.length) rays.last._2
        else rays(place)._2 // XXX interpolate
      }
    }

    def callback(col: Int, row: Int) = {
      if (col == startCol && row == startRow) { // starting point
        alpha = -180.0
        viewshed.setDouble(col, row, 1)
      }
      else { // any other point
        val deltax = startCol-col
        val deltay = startRow-row
        val distance = math.sqrt(deltax*deltax + deltay*deltay) * resolution
        val angle = math.atan((tile.getDouble(col, row) - viewHeight) / distance)

        if (alpha <= angle) {
          alpha = angle
          viewshed.setDouble(col, row, 1)
        }
      }
    }

    if (0 <= startCol && startCol < cols && 0 <= startRow && startRow < rows) { // starting point is in this tile
      var col = 0; while (col < cols) {
        slope = startRow.toDouble / (startCol - col)
        Rasterizer.foreachCellInGridLine(startCol, startRow, col, 0, null, re, false)(callback) // north
        slope = (startRow - rows + 1).toDouble / (startCol - col)
        Rasterizer.foreachCellInGridLine(startCol, startRow, col, rows-1, null, re, false)(callback) // south
        col += 1
      }
      var row = 0; while (row < rows) {
        slope = (startRow - row).toDouble / (startCol - cols + 1)
        Rasterizer.foreachCellInGridLine(startCol, startRow, cols-1, row, null, re, false)(callback) // east
        slope = (startRow - row).toDouble / startCol
        Rasterizer.foreachCellInGridLine(startCol, startRow, 0, row, null, re, false)(callback) // west
        row += 1
      }
    }
    else { // starting point is outside of this tile
      if (startCol < cols-1) { // right side
        val leftCol = 0
        val rightCol = cols-1
        var rightRow = 0; while (rightRow < rows) {
          slope = (startRow - rightRow).toDouble / (startCol - rightCol)
          alpha = slopeToAlpha(slope)
          val leftRow = math.round(slope * (leftCol - startCol) - startRow).toInt
          Rasterizer.foreachCellInGridLine(leftCol, leftRow, rightCol, rightRow, null, re, false)(callback)
          rightRow += 1
        }
      }
      if (startCol > 0) { // left side
        val leftCol = 0
        val rightCol = cols-1
        var leftRow = 0; while (leftRow < rows) {
          slope = (startRow - leftRow).toDouble / (startCol - leftCol)
          alpha = slopeToAlpha(slope)
          val rightRow = math.round(slope * (rightCol - startCol) - startRow).toInt
          Rasterizer.foreachCellInGridLine(rightCol, rightRow, leftCol, leftRow, null, re, false)(callback)
          leftRow += 1
        }
      }
      if (startRow < rows-1) { // bottom side
        val topRow = 0
        val bottomRow = rows-1
        var bottomCol = 0; while (bottomCol < cols) {
          val topCol =
            if (startCol == bottomCol) { // infinite slope
              alpha = slopeToAlpha(Double.PositiveInfinity)
              bottomCol
            }
            else { // finite slope
              slope = (startRow - bottomRow).toDouble / (startCol - bottomCol)
              alpha = slopeToAlpha(slope)
              math.round(((topRow - startRow) / slope) + startCol).toInt
            }
          Rasterizer.foreachCellInGridLine(topCol, topRow, bottomCol, bottomRow, null, re, false)(callback)
          bottomCol += 1
        }
      }
      if (startRow > 0) { // top side
        val topRow = 0
        val bottomRow = rows-1
        var topCol = 0; while (topCol < cols) {
          val bottomCol =
            if (startCol == topCol) { // infinite slope
              alpha = slopeToAlpha(Double.PositiveInfinity)
              topCol
            }
            else { //finite slope
              slope = (startRow - topRow).toDouble / (startCol - topCol)
              alpha = slopeToAlpha(slope)
              math.round(((bottomRow - startRow) / slope) + startCol).toInt
            }
          Rasterizer.foreachCellInGridLine(bottomCol, bottomRow, topCol, topRow, null, re, false)(callback)
          topCol += 1
        }
      }
    }

    viewshed
  }

}
