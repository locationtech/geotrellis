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
 * Gets minimum values.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns NoData.
 */
object Min extends LocalTileBinaryOp {
  def combine(z1:Int,z2:Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else math.min(z1,z2)

  def combine(z1:Double,z2:Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else math.min(z1,z2)
}

trait MinMethods extends TileMethods {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int): Tile = Min(tile, i)
  /** Min a constant Double value to each cell. */
  def localMin(d: Double): Tile = Min(tile, d)
  /** Min the values of each cell in each raster.  */
  def localMin(r:Tile): Tile = Min(tile, r)
  /** Min the values of each cell in each raster.  */
  def localMin(rs:Traversable[Tile]): Tile = Min(tile +: rs.toSeq)
}
