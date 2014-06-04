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
 * Multiplies values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Multiply extends LocalTileBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 * z2

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 * z2
}

trait MultiplyMethods { self: Tile =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int): Tile = Multiply(self, i)
  /** Multiply a constant value from each cell.*/
  def *(i: Int): Tile = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i: Int): Tile = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double): Tile = Multiply(self, d)
  /** Multiply a double constant value from each cell.*/
  def *(d: Double): Tile = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d: Double): Tile = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(r: Tile): Tile = Multiply(self,r)
  /** Multiply the values of each cell in each raster. */
  def *(r: Tile): Tile = localMultiply(r)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rs: Seq[Tile]): Tile = Multiply(self +: rs)
  /** Multiply the values of each cell in each raster. */
  def *(rs: Seq[Tile]): Tile = localMultiply(rs)
}
