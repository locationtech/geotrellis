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

import scala.annotation.tailrec

/**
 * Operation to add values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Add extends LocalTileBinaryOp {
  def combine(z1:Int,z2:Int) = 
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 + z2

  def combine(z1:Double,z2:Double) = 
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 + z2
}

trait AddMethods { self: Tile =>
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int): Tile = Add(self, i)
  /** Add a constant Int value to each cell. */
  def +(i: Int): Tile = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i: Int): Tile = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double): Tile = Add(self, d)
  /** Add a constant Double value to each cell. */
  def +(d: Double): Tile = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double): Tile = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(r: Tile): Tile = Add(self, r)
  /** Add the values of each cell in each raster. */
  def +(r: Tile): Tile = localAdd(r)
  /** Add the values of each cell in each raster.  */
  def localAdd(rs: Seq[Tile]): Tile = Add(self +: rs)
  /** Add the values of each cell in each raster. */
  def +(rs: Seq[Tile]): Tile = localAdd(rs)
}
