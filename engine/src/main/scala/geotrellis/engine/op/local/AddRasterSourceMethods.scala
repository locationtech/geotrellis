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

package geotrellis.engine.op.local

import geotrellis.engine._
import geotrellis.raster.op.local._

import scala.annotation.tailrec

trait AddRasterSourceMethods extends RasterSourceMethods {
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int): RasterSource = rasterSource.mapTile(Add(_, i))
  /** Add a constant Int value to each cell. */
  def +(i: Int): RasterSource = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +: (i: Int): RasterSource = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double): RasterSource = rasterSource.mapTile(Add(_, d))
  /** Add a constant Double value to each cell. */
  def +(d: Double): RasterSource = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +: (d: Double): RasterSource = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(rs: RasterSource): RasterSource = rasterSource.combineTile(rs)(Add(_, _))
  /** Add the values of each cell in each raster. */
  def +(rs: RasterSource): RasterSource = localAdd(rs)
  /** Add the values of each cell in each raster.  */
  def localAdd(rss: Seq[RasterSource]): RasterSource = rasterSource.combineTile(rss)(Add(_))
  /** Add the values of each cell in each raster. */
  def +(rss: Seq[RasterSource]): RasterSource = localAdd(rss)
}
