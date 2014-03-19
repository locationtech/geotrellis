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
 * Xor's cell values of rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */
object Xor extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) = 
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 ^ z2

  def combine(z1:Double,z2:Double) = 
    if(isNoData(z1) || isNoData(z2)) Double.NaN
    else i2d(d2i(z1) ^ d2i(z2))
}

trait XorOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) = self.mapOp(Xor(_, i))
  /** Xor a constant Int value to each cell. */
  def ^(i:Int) = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i:Int) = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(rs:RasterSource) = self.combineOp(rs)(Xor(_,_))
  /** Xor the values of each cell in each raster. */
  def ^(rs:RasterSource) = localXor(rs)
  /** Xor the values of each cell in each raster. */
  def localXor(rss:Seq[RasterSource]) = self.combineOp(rss)(Xor(_))
  /** Xor the values of each cell in each raster. */
  def ^(rss:Seq[RasterSource]) = localXor(rss)
}

trait XorMethods { self: Raster =>
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) = Xor(self, i)
  /** Xor a constant Int value to each cell. */
  def ^(i:Int) = localXor(i)
  /** Xor a constant Int value to each cell. */
  def ^:(i:Int) = localXor(i)
  /** Xor the values of each cell in each raster.  */
  def localXor(r:Raster) = Xor(self,r)
  /** Xor the values of each cell in each raster. */
  def ^(r:Raster) = localXor(r)
  /** Xor the values of each cell in each raster. */
  def localXor(rs:Seq[Raster]) = Xor(self +: rs)
  /** Xor the values of each cell in each raster. */
  def ^(rs:Seq[Raster]) = localXor(rs)
}
