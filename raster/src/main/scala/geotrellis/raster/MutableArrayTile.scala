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

import spire.syntax.cfor._


/**
  * [[MutableArrayTile]] is an [[ArrayTile]] whose cells can be
  * written to (mutated).
  */
abstract class MutableArrayTile extends ArrayTile {
  def mutable = this

  /**
   * cols and rows are explicitly defined to help with the Grid[N].{cols | rows} specialized functions dispatch.
   * See https://github.com/locationtech/geotrellis/issues/3427
   */
  def cols: Int

  def rows: Int

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def update(i:Int, z:Int): Unit

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def updateDouble(i:Int, z:Double):Unit

  /**
    * Set the value of the raster at the given column and row with the
    * given value.
    *
    * @param  col    The column
    * @param  row    The row
    * @param  value  The value
    */
  def set(col:Int, row:Int, value:Int): Unit = {
    update(row * cols + col, value)
  }

  /**
    * Set the value of the raster at the given column and row with the
    * given value.
    *
    * @param  col    The column
    * @param  row    The row
    * @param  value  The value
    */
  def setDouble(col:Int, row:Int, value:Double): Unit = {
    updateDouble(row * cols + col, value)
  }

  /**
    * Paste the given [[Tile]] into the present one starting at the
    * given column and row offsets.
    *
    * @param  colOffset  The column offset
    * @param  rowOffset  The row offset
    * @param  update     The source tile
    */
  def update(colOffset:Int, rowOffset:Int, update: Tile): Unit = {
    if (this.cellType.isFloatingPoint) {
      cfor(0)(_ < update.rows, _ + 1) { r =>
        cfor(0)(_ < update.cols, _ + 1) { c =>
          setDouble(c + colOffset, r + rowOffset, update.getDouble(c, r))
        }
      }
    } else {
      cfor(0)(_ < update.rows, _ + 1) { r =>
        cfor(0)(_ < update.cols, _ + 1) { c =>
          set(c + colOffset, r + rowOffset, update.get(c, r))
        }
      }
    }
  }
}
