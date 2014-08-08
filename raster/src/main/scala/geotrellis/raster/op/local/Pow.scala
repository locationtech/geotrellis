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
 * Pows values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Pow extends LocalTileBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else if (z2 == 0) NODATA
    else math.pow(z1,z2).toInt

  def combine(z1:Double,z2:Double) =
    math.pow(z1,z2)
}

trait PowMethods extends TileMethods {
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): Tile = Pow(tile, i)
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int): Tile = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i:Int): Tile = Pow(i, tile)
  /** Pow a constant value by each cell value.*/
  def **:(i:Int): Tile = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): Tile = Pow(tile, d)
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double): Tile = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d:Double): Tile = Pow(d,tile)
  /** Pow a double constant value by each cell value.*/
  def **:(d:Double): Tile = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(r:Tile): Tile = Pow(tile,r)
  /** Pow the values of each cell in each raster. */
  def **(r:Tile): Tile = localPow(r)
  /** Pow the values of each cell in each raster. */
  def localPow(rs:Seq[Tile]): Tile = Pow(tile +: rs)
  /** Pow the values of each cell in each raster. */
  def **(rs:Seq[Tile]): Tile = localPow(rs)
}
