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
import geotrellis.source._

/**
 * Subtracts values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Subtract extends LocalTileBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 - z2

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 - z2
}

trait SubtractOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int): RasterSource = self.mapOp(Subtract(_, i))
  /** Subtract a constant value from each cell.*/
  def -(i:Int) = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int): RasterSource = self.mapOp(Subtract(i, _))
  /** Subtract each value of a cell from a constant value. */
  def -:(i:Int) = localSubtract(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double): RasterSource = self.mapOp(Subtract(_, d))
  /** Subtract a double constant value from each cell.*/
  def -(d:Double) = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double): RasterSource = self.mapOp(Subtract(d, _))
  /** Subtract each value of a cell from a double constant value. */
  def -:(d:Double) = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rs:RasterSource): RasterSource = self.combineOp(rs)(Subtract(_,_))
  /** Subtract the values of each cell in each raster. */
  def -(rs:RasterSource): RasterSource = localSubtract(rs)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rss:Seq[RasterSource]): RasterSource = self.combineOp(rss)(Subtract(_))
  /** Subtract the values of each cell in each raster. */
  def -(rss:Seq[RasterSource]): RasterSource = localSubtract(rss)
}

trait SubtractMethods { self: Tile =>
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int): Tile = Subtract(self, i)
  /** Subtract a constant value from each cell.*/
  def -(i: Int): Tile = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int): Tile = Subtract(i, self)
  /** Subtract each value of a cell from a constant value. */
  def -:(i: Int): Tile = localSubtract(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double): Tile = Subtract(self, d)
  /** Subtract a double constant value from each cell.*/
  def -(d: Double): Tile = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double): Tile = Subtract(d, self)
  /** Subtract each value of a cell from a double constant value. */
  def -:(d: Double): Tile = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(r: Tile): Tile = Subtract(self, r)
  /** Subtract the values of each cell in each raster. */
  def -(r: Tile): Tile = localSubtract(r)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(rs: Seq[Tile]): Tile = Subtract(self +: rs)
  /** Subtract the values of each cell in each raster. */
  def -(rs: Seq[Tile]): Tile = localSubtract(rs)
}
