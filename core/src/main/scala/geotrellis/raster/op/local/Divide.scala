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
 * Divides values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Divide extends LocalTileBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else if (z2 == 0) NODATA
    else z1 / z2

  def combine(z1:Double,z2:Double) =
    if (z2 == 0) Double.NaN
    else z1 / z2
}

trait DivideMethods { self: Tile =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int): Tile = Divide(self, i)
  /** Divide each value of the raster by a constant value.*/
  def /(i: Int): Tile = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int): Tile = Divide(i,self)
  /** Divide a constant value by each cell value.*/
  def /:(i: Int): Tile = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double): Tile = Divide(self, d)
  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double): Tile = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double): Tile = Divide(d,self)
  /** Divide a double constant value by each cell value.*/
  def /:(d: Double): Tile = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(r:Tile): Tile = Divide(self, r)
  /** Divide the values of each cell in each raster. */
  def /(r: Tile): Tile = localDivide(r)
  /** Divide the values of each cell in each raster. */
  def localDivide(rs: Seq[Tile]): Tile = Divide(self +: rs)
  /** Divide the values of each cell in each raster. */
  def /(rs: Seq[Tile]): Tile = localDivide(rs)
}
