/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._

/**
 * Gets maximum values.
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns NoData.
 */
object Max extends LocalRasterBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else math.max(z1,z2)

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else math.max(z1,z2)
}

trait MaxOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Max a constant Int value to each cell. */
  def localMax(i: Int) = self.mapOp(Max(_, i))
  /** Max a constant Double value to each cell. */
  def localMax(d: Double) = self.mapOp(Max(_, d))
  /** Max the values of each cell in each raster.  */
  def localMax(rs:RasterSource) = self.combineOp(rs)(Max(_,_))
  /** Max the values of each cell in each raster.  */
  def localMax(rss:Seq[RasterSource]) = self.combineOp(rss)(Max(_))
}

trait MaxMethods { self: Raster =>
  /** Max a constant Int value to each cell. */
  def localMax(i: Int) = Max(self, i)
  /** Max a constant Double value to each cell. */
  def localMax(d: Double) = Max(self, d)
  /** Max the values of each cell in each raster.  */
  def localMax(r:Raster) = Max(self, r)
  /** Max the values of each cell in each raster.  */
  def localMax(rs:Seq[Raster]) = Max(self +: rs)
}
