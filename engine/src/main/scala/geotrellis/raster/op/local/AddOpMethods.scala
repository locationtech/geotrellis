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
import geotrellis.raster._

import scala.annotation.tailrec

trait AddOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int): RasterSource = self.mapTile(Add(_, i))
  /** Add a constant Int value to each cell. */
  def +(i: Int): RasterSource = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +: (i: Int): RasterSource = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double): RasterSource = self.mapTile(Add(_, d))
  /** Add a constant Double value to each cell. */
  def +(d: Double): RasterSource = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +: (d: Double): RasterSource = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(rs: RasterSource): RasterSource = self.combineTile(rs)(Add(_, _))
  /** Add the values of each cell in each raster. */
  def +(rs: RasterSource): RasterSource = localAdd(rs)
  /** Add the values of each cell in each raster.  */
  def localAdd(rss: Seq[RasterSource]): RasterSource = self.combineTile(rss)(Add(_))
  /** Add the values of each cell in each raster. */
  def +(rss: Seq[RasterSource]): RasterSource = localAdd(rss)
}
