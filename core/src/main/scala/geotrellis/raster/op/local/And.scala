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
 * Operation to And values.
 * 
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */
object And extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 & z2

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else i2d(d2i(z1) & d2i(z2))
}

trait AndOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** And a constant Int value to each cell. */
  def localAnd(i: Int): RasterSource = self.mapOp(And(_, i))
  /** And a constant Int value to each cell. */
  def &(i:Int): RasterSource = localAnd(i)
  /** And a constant Int value to each cell. */
  def &:(i:Int): RasterSource = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(rs:RasterSource): RasterSource = self.combineOp(rs)(And(_,_))
  /** And the values of each cell in each raster. */
  def &(rs:RasterSource): RasterSource = localAnd(rs)
  /** And the values of each cell in each raster.  */
  def localAnd(rss:Seq[RasterSource]): RasterSource = self.combineOp(rss)(And(_))
  /** And the values of each cell in each raster. */
  def &(rss:Seq[RasterSource]): RasterSource = localAnd(rss)
}

trait AndMethods { self: Raster =>
  /** And a constant Int value to each cell. */
  def localAnd(i: Int): Raster = And(self, i)
  /** And a constant Int value to each cell. */
  def &(i:Int): Raster = localAnd(i)
  /** And a constant Int value to each cell. */
  def &:(i:Int): Raster = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(r:Raster): Raster = And(self, r)
  /** And the values of each cell in each raster. */
  def &(r:Raster): Raster = localAnd(r)
  /** And the values of each cell in each raster.  */
  def localAnd(rs:Seq[Raster]): Raster = And(self +: rs)
  /** And the values of each cell in each raster. */
  def &(rs:Seq[Raster]): Raster = localAnd(rs)
}
