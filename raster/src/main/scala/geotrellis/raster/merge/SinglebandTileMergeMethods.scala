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
  def merge(other: Tile, baseCol: Int, baseRow: Int): Tile = {
    val mutableTile = self.mutable
    Seq(self, other).assertEqualDimensions()
    self.cellType match {
      case BitCellType =>
        cfor(0)(_ < other.rows, _ + 1) { row =>
          cfor(0)(_ < other.cols, _ + 1) { col =>
            if (other.get(col, row) == 1) {
              mutableTile.set(col + baseCol, row + baseRow, 1)
            }
          }
        }
      case ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
        // Assume 0 as the transparent value
        cfor(0)(_ < other.rows, _ + 1) { row =>
          cfor(0)(_ < other.cols, _ + 1) { col =>
            if (self.get(col + baseCol, row + baseRow) == 0) {
              mutableTile.set(col + baseCol, row + baseRow, other.get(col, row))
            }
          }
        }
      case FloatCellType | DoubleCellType =>
        // Assume 0.0 as the transparent value
        cfor(0)(_ < other.rows, _ + 1) { row =>
          cfor(0)(_ < other.cols, _ + 1) { col =>
            if (self.getDouble(col + baseCol, row + baseRow) == 0.0) {
              mutableTile.setDouble(col + baseCol, row + baseRow, other.getDouble(col, row))
            }
          }
        }
      case x if x.isFloatingPoint =>
        cfor(0)(_ < other.rows, _ + 1) { row =>
          cfor(0)(_ < other.cols, _ + 1) { col =>
            if (isNoData(self.getDouble(col + baseCol, row + baseRow))) {
              mutableTile.setDouble(col + baseCol, row + baseRow, other.getDouble(col, row))
            }
          }
        }
      case _ =>
        cfor(0)(_ < other.rows, _ + 1) { row =>
          cfor(0)(_ < other.cols, _ + 1) { col =>
            if (isNoData(self.get(col + baseCol, row + baseRow))) {
              mutableTile.set(col + baseCol, row + baseRow, other.get(col, row))
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
        val gridBounds = re.gridBoundsFor(sharedExtent)
        val targetCS = CellSize(sharedExtent, gridBounds.width, gridBounds.height)

        val interpolate = Resample(method, other, otherExtent, targetCS)

        self.cellType match {
          case BitCellType | ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (self.get(col, row) == 0) {
                  val (x, y) = re.gridToMap(col, row)
                  val v = interpolate.resample(x, y)
                  if(isData(v)) {
                    mutableTile.set(col, row, v)
                  }
                }
              }
            }
          case FloatCellType | DoubleCellType =>
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (self.getDouble(col, row) == 0.0) {
                  val (x, y) = re.gridToMap(col, row)
                  val v = interpolate.resampleDouble(x, y)
                  if(isData(v)) {
                    mutableTile.setDouble(col, row, v)
                  }
                }
              }
            }
          case x if x.isFloatingPoint =>
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (isNoData(self.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.setDouble(col, row, interpolate.resampleDouble(x, y))
                }
              }
            }
          case _ =>
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (isNoData(self.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.set(col, row, interpolate.resample(x, y))
                }
              }
            }
        }

        mutableTile
      case _ =>
        self
    }

  /** Unions this tile with another tile, preserving the non-overlapping
    * portions of each tile extent.
    *
    * This method requires a union function to be provided which decides
    * how values will be added to the output raster. A `None` represents
    * one of the two input rasters not having a value at the resampled location.
    * Two `None` values mean that both tiles are missing values at the
    * cell in question.
    */
  def union(extent: Extent, otherExtent: Extent, other: Tile, method: ResampleMethod, unionFunc: (Option[Double], Option[Double]) => Double): Tile = {
    val unionInt: (Option[Int], Option[Int]) => Int =
      (l: Option[Int], r: Option[Int]) => {
        unionFunc(l.map(_.toDouble), r.map(_.toDouble)).toInt
      }

    val combinedExtent = otherExtent combine extent
    val re = RasterExtent(extent, self.cols, self.rows)
    val gridBounds = re.gridBoundsFor(combinedExtent, false)
    val targetCS = CellSize(combinedExtent, gridBounds.width, gridBounds.height)
    val targetRE = RasterExtent(combinedExtent, targetCS)
    val mutableTile = ArrayTile.empty(self.cellType, targetRE.cols, targetRE.rows)

    val interpolateLeft = Resample(method, self, extent, targetCS)
    val interpolateRight = Resample(method, other, otherExtent, targetCS)

    self.cellType match {
      case BitCellType | ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
        cfor(0)(_ < targetRE.rows, _ + 1) { row =>
          cfor(0)(_ < targetRE.cols, _ + 1) { col =>
            val (x,y) = targetRE.gridToMap(col, row)
            val l = interpolateLeft.resample(x, y)
            val r = interpolateRight.resample(x, y)
            val maybeL = Some(l)
            val maybeR = Some(r)
            val v = unionInt(maybeL, maybeR)
            mutableTile.set(col, row, v)
          }
        }
      case FloatCellType | DoubleCellType =>
        cfor(0)(_ < targetRE.rows, _ + 1) { row =>
          cfor(0)(_ < targetRE.cols, _ + 1) { col =>
            val (x,y) = targetRE.gridToMap(col, row)
            val l = interpolateLeft.resampleDouble(x, y)
            val r = interpolateRight.resampleDouble(x, y)
            val maybeL = Some(l)
            val maybeR = Some(r)
            val v = unionFunc(maybeL, maybeR)
            mutableTile.setDouble(col, row, v)
          }
        }
      case x if x.isFloatingPoint =>
        cfor(0)(_ < targetRE.rows, _ + 1) { row =>
          cfor(0)(_ < targetRE.cols, _ + 1) { col =>
            val (x,y) = targetRE.gridToMap(col, row)
            val l = interpolateLeft.resampleDouble(x, y)
            val r = interpolateRight.resampleDouble(x, y)
            val maybeL = if (isNoData(l)) None else Some(l)
            val maybeR = if (isNoData(r)) None else Some(r)
            mutableTile.setDouble(col, row, unionFunc(maybeL, maybeR))
          }
        }
      case _ =>
        cfor(0)(_ < targetRE.rows, _ + 1) { row =>
          cfor(0)(_ < targetRE.cols, _ + 1) { col =>
            val (x,y) = targetRE.gridToMap(col, row)
            val l = interpolateLeft.resample(x, y)
            val r = interpolateRight.resample(x, y)
            val maybeL = if (isNoData(l)) None else Some(l)
            val maybeR = if (isNoData(r)) None else Some(r)
            mutableTile.set(col, row, unionInt(maybeL, maybeR))
          }
        }
    }
    mutableTile
  }
}
