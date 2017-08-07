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

package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{Resample, ResampleMethod}
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * Trait containing extension methods for doing merge operations on
  * single-band [[Tile]]s.
  */
trait SinglebandTileMergeMethods extends TileMergeMethods[Tile] {
  /** Merges this tile with another tile.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note                         This method requires that the dimensions be the same between the tiles, and assumes
    *                               equal extents.
    */
  def merge(other: Tile): Tile = {
    Seq(self, other).assertEqualDimensions()
    merge(other, GridBounds(0,0, self.cols - 1, self.rows - 1))
  }

  /** Merge this tile with another for given bounds.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note It is assumed that other tile cell (0,0) lines up with (0,0) of this tile.
    *
    * @param other     Source of cells to be merged
    * @param bounds    Cell grid bounds in target tile for merge operation
    */
  def merge(other: Tile, bounds: GridBounds): Tile = {
    require(other.cols <= bounds.width && other.rows <= bounds.height,
      s"Not have enough cells ${self.dimensions} for $bounds")
    merge(other, bounds, bounds.colMin, bounds.rowMin)
  }

  /** Merge this tile with another for given bounds.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note
    * Offset of (0,0) indicates that upper left corner of other tile lines up with
    * upper left corner of the merge bounds. Incrementing the offset will have the
    * effect of moving other tile up and to the left relative to this tile.
    *
    * @param other     Source of cells to be merged
    * @param bounds    Cell grid bounds in target tile for merge operation
    * @param colOffset Offset of row 0 in other tile relative to merge bounds
    * @param rowOffset Offset or col 0 in other tile relative to merge bounds
    */
  def merge(other: Tile, bounds: GridBounds, colOffset: Int, rowOffset: Int) = {
    val mutableTile = self.mutable
    val GridBounds(colMin, rowMin, colMax, rowMax) = bounds

    self.cellType match {
      case BitCellType =>
        cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
          cfor(colMin)(_ <= colMax, _ + 1) { col =>
            if (other.get(col - colOffset, row - rowOffset) == 1) {
              mutableTile.set(col, row, 1)
            }
          }
        }
      case ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
        // Assume 0 as the transparent value
        cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
          cfor(colMin)(_ <= colMax, _ + 1) { col =>
            if (self.get(col, row) == 0) {
              mutableTile.set(col, row, other.get(col - colOffset, row - rowOffset))
            }
          }
        }
      case FloatCellType | DoubleCellType =>
        // Assume 0.0 as the transparent value
        cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
          cfor(colMin)(_ <= colMax, _ + 1) { col =>
            if (self.getDouble(col, row) == 0.0) {
              mutableTile.setDouble(col, row, other.getDouble(col - colOffset, row - rowOffset))
            }
          }
        }
      case x if x.isFloatingPoint =>
        cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
          cfor(colMin)(_ <= colMax, _ + 1) { col =>
            if (isNoData(self.getDouble(col, row))) {
              mutableTile.setDouble(col, row, other.getDouble(col - colOffset, row - rowOffset))
            }
          }
        }
      case _ =>
        cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
          cfor(colMin)(_ <= colMax, _ + 1) { col =>
            if (isNoData(self.get(col, row))) {
              mutableTile.set(col, row, other.get(col - colOffset, row - rowOffset))
            }
          }
        }
    }

    mutableTile
  }

  /** Merges this tile with another tile, given the extents both tiles.
    *
    * This method will replace the values of these cells with a
    * resampled value taken from the tile's cells, if the source cell
    * is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    */
  def merge(extent: Extent, otherExtent: Extent, other: Tile, method: ResampleMethod): Tile =
    otherExtent & extent match {
      case Some(sharedExtent) =>
        val mutableTile = self.mutable
        val re = RasterExtent(extent, self.cols, self.rows)
        val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
        val targetCS = CellSize(sharedExtent, colMax, rowMax)

        self.cellType match {
          case BitCellType | ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resample _
            // Assume 0 as the transparent value
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if (self.get(col, row) == 0) {
                  val (x, y) = re.gridToMap(col, row)
                  val v = interpolate(x, y)
                  if(isData(v)) {
                    mutableTile.set(col, row, v)
                  }
                }
              }
            }
          case FloatCellType | DoubleCellType =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resampleDouble _
            // Assume 0.0 as the transparent value
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if (self.getDouble(col, row) == 0.0) {
                  val (x, y) = re.gridToMap(col, row)
                  val v = interpolate(x, y)
                  if(isData(v)) {
                    mutableTile.setDouble(col, row, v)
                  }
                }
              }
            }
          case x if x.isFloatingPoint =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resampleDouble _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if (isNoData(self.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.setDouble(col, row, interpolate(x, y))
                }
              }
            }
          case _ =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resample _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if (isNoData(self.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.set(col, row, interpolate(x, y))
                }
              }
            }
        }

        mutableTile
      case _ =>
        self
    }
}
