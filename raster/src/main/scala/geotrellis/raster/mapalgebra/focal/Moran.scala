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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.summary.Statistics
import geotrellis.raster.histogram.FastMapHistogram

/**
 * Calculates spatial autocorrelation of cells based on the similarity to
 * neighboring values.
 *
 * The statistic for each focus in the resulting raster is such that the more
 * positive the number, the greater the similarity between the focus value and
 * it's neighboring values, and the more negative the number, the more dissimilar
 * the focus value is with it's neighboring values.
 *
 * @note                  This operation requires that the whole raster be passed in;
 *                        it does not work over tiles.
 *
 * @note                  Since mean and standard deviation are based off of an
 *                        Int based Histogram, those values will come from rounded values
 *                        of a double typed Tile (FloatConstantNoDataCellType, DoubleConstantNoDataCellType).
 */
object TileMoransICalculation {
  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell = TargetCell.All): Tile = {
    new CursorCalculation[Tile](tile, n, bounds, target)
      with DoubleArrayTileResult
    {

      var mean = 0.0
      var `stddev^2` = 0.0

      val h = FastMapHistogram.fromTile(r)
      val stats = h.statistics
      require(stats.nonEmpty)
      val Statistics(_, m, _, _, s, _, _) = stats.get
      mean = m
      `stddev^2` = s * s

      def calc(r: Tile, cursor: Cursor) = {
        var z = 0.0
        var w = 0
        var base = 0.0

        cursor.allCells.foreach { (x, y) =>
          if(x == cursor.col && y == cursor.row) {
            base = r.getDouble(x, y) - mean
          } else {
            z += r.getDouble(x, y) - mean
            w += 1
          }
        }

        resultTile.setDouble(cursor.col, cursor.row, (base / `stddev^2` * z) / w)
      }
    }
  }.execute()
}

/**
 * Calculates global spatial autocorrelation of a raster based on the similarity to
 * neighboring values.
 *
 * The resulting statistic is such that the more positive the number, the greater
 * the similarity of values in the raster, and the more negative the number,
 * the more dissimilar the raster values are.
 *
 * @note                  This operation requires that the whole raster be passed in;
 *                        it does not work over tiles.
 *
 * @note                  Since mean and standard deviation are based off of an
 *                        Int based Histogram, those values will come from rounded values
 *                        of a double typed Tile (FloatConstantNoDataCellType, DoubleConstantNoDataCellType).
 */
object ScalarMoransICalculation {
  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds]): Double = {
    new CursorCalculation[Double](tile, n, bounds, TargetCell.All) {
      var mean: Double = 0
      var `stddev^2`: Double = 0

      var count: Double = 0.0
      var ws: Int = 0

      val h = FastMapHistogram.fromTile(r)
      val stats = h.statistics
      require(stats.nonEmpty)
      val Statistics(_, m, _, _, s, _, _) = stats.get
      mean = m
      `stddev^2` = s * s

      def calc(r: Tile, cursor: Cursor) = {
        val base = r.getDouble(cursor.col, cursor.row) - mean
        var z = -base

        cursor.allCells.foreach { (x, y) => z += r.getDouble(x, y) - mean; ws += 1}

        count += base / `stddev^2` * z
        ws -= 1 // subtract one to account for focus
      }

      def result = count / ws

      // Hack to make it work with the focal architecture, in which it clearly does not quite fit.
      val copyOriginalValue: (Int, Int, Int, Int) => Unit = { (focusCol: Int, focusRow: Int, col: Int, row: Int) =>
        ???
      }
    }
  }.execute()
}
