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
import geotrellis.source._

/**
 * Pows values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */
object Pow extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else if (z2 == 0) NODATA
    else math.pow(z1,z2).toInt

  def combine(z1:Double,z2:Double) =
    math.pow(z1,z2)
}

trait PowOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): RasterSource = self.mapOp(Pow(_, i))
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int): RasterSource = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i:Int): RasterSource = self.mapOp(Pow(i,_))
  /** Pow a constant value by each cell value.*/
  def **:(i:Int): RasterSource = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): RasterSource = self.mapOp(Pow(_, d))
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double): RasterSource = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d:Double): RasterSource = self.mapOp(Pow(d,_))
  /** Pow a double constant value by each cell value.*/
  def **:(d:Double): RasterSource = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(rs:RasterSource): RasterSource = self.combineOp(rs)(Pow(_,_))
  /** Pow the values of each cell in each raster. */
  def **(rs:RasterSource): RasterSource = localPow(rs)
  /** Pow the values of each cell in each raster. */
  def localPow(rss:Seq[RasterSource]): RasterSource = self.combineOp(rss)(Pow(_))
  /** Pow the values of each cell in each raster. */
  def **(rss:Seq[RasterSource]): RasterSource = localPow(rss)
}

trait PowMethods { self: Raster =>
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): Raster = Pow(self, i)
  /** Pow each value of the raster by a constant value.*/
  def **(i:Int): Raster = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i:Int): Raster = Pow(i, self)
  /** Pow a constant value by each cell value.*/
  def **:(i:Int): Raster = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): Raster = Pow(self, d)
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double): Raster = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d:Double): Raster = Pow(d,self)
  /** Pow a double constant value by each cell value.*/
  def **:(d:Double): Raster = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(r:Raster): Raster = Pow(self,r)
  /** Pow the values of each cell in each raster. */
  def **(r:Raster): Raster = localPow(r)
  /** Pow the values of each cell in each raster. */
  def localPow(rs:Seq[Raster]): Raster = Pow(self +: rs)
  /** Pow the values of each cell in each raster. */
  def **(rs:Seq[Raster]): Raster = localPow(rs)
}
