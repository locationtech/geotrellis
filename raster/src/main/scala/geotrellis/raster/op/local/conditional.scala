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
 * Maps all cells matching `cond` to Int `trueValue`.
 */
object IfCell extends Serializable {
  /**
   * Maps all cells matching `cond` to Int `trueValue`.
   */
  def apply(r: Tile, cond: Int => Boolean, trueValue: Int): Tile =
    r.dualMap {z: Int => if (cond(z)) trueValue else z }
              {z: Double => if (cond(d2i(z))) i2d(trueValue) else z}

  /**
   * Maps all cells matching `cond` to Double `trueValue`.
   */
  def apply(r: Tile, cond: Double => Boolean, trueValue: Double): Tile =
    r.dualMap {z: Int => if (cond(i2d(z))) d2i(trueValue) else z}
              {z: Double => if (cond(z)) trueValue else z}

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def apply(r: Tile, cond: Int => Boolean, trueValue: Int, falseValue: Int): Tile =
    r.dualMap {z: Int => if (cond(z)) trueValue else falseValue}
              {z: Double => if (cond(d2i(z))) i2d(trueValue) else i2d(falseValue)}

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false for Double values.  */
  def apply(r: Tile, cond: Double => Boolean, trueValue: Double, falseValue: Double): Tile =
    r.dualMap {z: Int => if (cond(i2d(z))) d2i(trueValue) else d2i(falseValue) }
              {z: Double => if (cond(z)) trueValue else falseValue }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1: Tile, r2: Tile, cond: (Int, Int) => Boolean, trueValue: Int): Tile =
    r1.dualCombine(r2) { (z1, z2) => if (cond(z1, z2)) trueValue else z1 }
                       { (z1, z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else z1 }

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1: Tile, r2: Tile, cond: (Double, Double) => Boolean, trueValue: Double): Tile =
    r1.dualCombine(r2) { (z1, z2) => if (cond(i2d(z1), i2d(z2))) d2i(trueValue) else z1 }
                       { (z1, z2) => if (cond(z1, z2)) trueValue else z1 }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input rasters.*/
  def apply(r1: Tile, r2: Tile, cond: (Int, Int) => Boolean, trueValue: Int, falseValue: Int): Tile =
    r1.dualCombine(r2) { (z1, z2) => if(cond(z1, z2)) trueValue else falseValue }
                       { (z1, z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else i2d(falseValue) }

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input rasters.*/
  def apply(r1: Tile, r2: Tile, cond: (Double, Double) => Boolean, trueValue: Double, falseValue: Double): Tile =
    r1.dualCombine(r2) { (z1, z2) => if(cond(i2d(z1), i2d(z2))) d2i(trueValue) else d2i(falseValue) }
                       { (z1, z2) => if (cond(z1, z2)) trueValue else falseValue }
}

trait ConditionalMethods extends TileMethods {
  def localIf(cond: Int => Boolean, trueValue: Int): Tile =
    IfCell(tile, cond, trueValue)
  def localIf(cond: Double => Boolean, trueValue: Double): Tile =
    IfCell(tile, cond, trueValue)
  def localIf(cond: Int => Boolean, trueValue: Int, falseValue: Int): Tile =
    IfCell(tile, cond, trueValue, falseValue)
  def localIf(cond: Double => Boolean, trueValue: Double, falseValue: Double): Tile =
    IfCell(tile, cond, trueValue, falseValue)
  def localIf(r: Tile, cond: (Int, Int) => Boolean, trueValue: Int): Tile =
    IfCell(tile, r, cond, trueValue)
  def localIf(r: Tile, cond: (Double, Double) => Boolean, trueValue: Double): Tile =
    IfCell(tile, r, cond, trueValue)
  def localIf(r: Tile, cond: (Int, Int) => Boolean, trueValue: Int, falseValue: Int): Tile =
    IfCell(tile, r, cond, trueValue, falseValue)
  def localIf(r: Tile, cond: (Double, Double) => Boolean, trueValue: Double, falseValue: Double): Tile =
    IfCell(tile, r, cond, trueValue, falseValue)
}
