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

trait GreaterOpMethods[+Repr <: RasterSource] { self: Repr =>
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int): RasterSource = self.map(Greater(_, i))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreaterRightAssociative(i: Int): RasterSource = self.map(Greater(i, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def >(i:Int): RasterSource = localGreater(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(i:Int): RasterSource = localGreaterRightAssociative(i)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double): RasterSource = self.map(Greater(_, d))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double): RasterSource = self.map(Greater(d, _))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def >(d:Double): RasterSource = localGreater(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   * 
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d:Double): RasterSource = localGreaterRightAssociative(d)
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def localGreater(rs:RasterSource): RasterSource = self.combine(rs)(Greater(_,_))
  /**
   * Returns a Raster with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are greater than the next raster, else 0.
   */
  def >(rs:RasterSource): RasterSource = localGreater(rs)
}
