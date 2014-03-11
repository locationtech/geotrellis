/***
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
 ***/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Divides values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Divide extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else if (z2 == 0) NODATA
    else z1 / z2

  def combine(z1:Double,z2:Double) =
    if (z2 == 0) Double.NaN
    else z1 / z2
}

trait DivideOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int): RasterSource = self.mapOp(Divide(_, i))
  /** Divide each value of the raster by a constant value.*/
  def /(i:Int) = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i:Int): RasterSource = self.mapOp(Divide(i,_))
  /** Divide a constant value by each cell value.*/
  def /:(i:Int): RasterSource = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double): RasterSource = self.mapOp(Divide(_, d))
  /** Divide each value of a raster by a double constant value.*/
  def /(d:Double): RasterSource = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d:Double): RasterSource = self.mapOp(Divide(d,_))
  /** Divide a double constant value by each cell value.*/
  def /:(d:Double): RasterSource = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(rs:RasterSource): RasterSource = self.combineOp(rs)(Divide(_,_))
  /** Divide the values of each cell in each raster. */
  def /(rs:RasterSource): RasterSource = localDivide(rs)
  /** Divide the values of each cell in each raster. */
  def localDivide(rss:Seq[RasterSource]): RasterSource = self.combineOp(rss)(Divide(_))
  /** Divide the values of each cell in each raster. */
  def /(rss:Seq[RasterSource]): RasterSource = localDivide(rss)
}

trait DivideMethods { self: Raster =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int): Raster = Divide(self, i)
  /** Divide each value of the raster by a constant value.*/
  def /(i: Int): Raster = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int): Raster = Divide(i,self)
  /** Divide a constant value by each cell value.*/
  def /:(i: Int): Raster = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double): Raster = Divide(self, d)
  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double): Raster = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double): Raster = Divide(d,self)
  /** Divide a double constant value by each cell value.*/
  def /:(d: Double): Raster = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(r:Raster): Raster = Divide(self, r)
  /** Divide the values of each cell in each raster. */
  def /(r: Raster): Raster = localDivide(r)
  /** Divide the values of each cell in each raster. */
  def localDivide(rs: Seq[Raster]): Raster = Divide(self +: rs)
  /** Divide the values of each cell in each raster. */
  def /(rs: Seq[Raster]): Raster = localDivide(rs)
}
