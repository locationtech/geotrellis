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

import geotrellis._
import geotrellis.raster._
import geotrellis.raster._

trait ConditionalOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Maps all cells matching `cond` to Int `trueValue`.*/
  def localIf(cond: Int => Boolean, trueValue: Int): RasterSource =
    map(IfCell(_, cond, trueValue), "IfCell")

  /** Maps all cells matching `cond` to Double `trueValue`.*/
  def localIf(cond: Double => Boolean, trueValue: Double): RasterSource =
    map(IfCell(_, cond, trueValue), "IfCell")

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def localIf(cond: Int => Boolean, trueValue: Int, falseValue: Int): RasterSource =
    map(IfCell(_, cond, trueValue, falseValue), "IfCell")

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false for Double values.  */
  def localIf(cond: Double => Boolean, trueValue: Double, falseValue: Double): RasterSource =
    map(IfCell(_, cond, trueValue, falseValue), "IfCell")

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def localIf(rs: RasterSource, cond: (Int, Int)=>Boolean, trueValue: Int): RasterSource =
    combine(rs, "BinaryIfCell")(IfCell(_, _, cond, trueValue))

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def localIf(rs: RasterSource, cond: (Double, Double)=>Boolean, trueValue: Double): RasterSource =
    combine(rs, "BinaryIfCell")(IfCell(_, _, cond, trueValue))

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input rasters.*/
  def localIf(rs: RasterSource, cond: (Int, Int)=>Boolean, trueValue: Int, falseValue: Int): RasterSource =
    combine(rs, "BinaryIfElseCell")(IfCell(_, _, cond, trueValue, falseValue))

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input rasters.*/
  def localIf(rs: RasterSource, cond: (Double, Double)=>Boolean, trueValue: Double, falseValue: Double): RasterSource =
    combine(rs, "BinaryIfElseCell")(IfCell(_, _, cond, trueValue, falseValue))
}
