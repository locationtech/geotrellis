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

package geotrellis.raster.op.local

import geotrellis.raster._

/**
 * Determines if values are less than other values. Sets to 1 if true, else 0.
 */
object Less extends LocalTileComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if(z1 < z2) true else false

  def compare(z1: Double, z2: Double): Boolean =
    if(isNoData(z1)) { false }
    else {
      if(isNoData(z2)) { false }
      else {
        if(z1 < z2) true
        else false
      }
    }
}

trait LessMethods extends TileMethods {
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLess(i: Int): Tile = Less(tile, i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def <(i: Int): Tile = localLess(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   */
  def localLessRightAssociative(i: Int): Tile = Less(i, tile)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * integer, else 0.
   *
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(i: Int): Tile = localLessRightAssociative(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def localLess(d: Double): Tile = Less(tile, d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def localLessRightAssociative(d: Double): Tile = Less(d, tile)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   */
  def <(d: Double): Tile = localLess(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is less than the input
   * double, else 0.
   *
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(d: Double): Tile = localLessRightAssociative(d)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than the next raster, else 0.
   */
  def localLess(r: Tile): Tile = Less(tile, r)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are less than the next raster, else 0.
   */
  def <(r: Tile): Tile = localLess(r)
}
