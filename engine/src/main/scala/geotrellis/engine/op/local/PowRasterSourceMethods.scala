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

trait PowRasterSourceMethods extends RasterSourceMethods {
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int): RasterSource = rasterSource.mapTile(Pow(_, i))
  /** Pow each value of the raster by a constant value.*/
  def **(i: Int): RasterSource = localPow(i)
  /** Pow a constant value by each cell value.*/
  def localPowValue(i: Int): RasterSource = rasterSource.mapTile(Pow(i, _))
  /** Pow a constant value by each cell value.*/
  def **:(i: Int): RasterSource = localPowValue(i)
  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double): RasterSource = rasterSource.mapTile(Pow(_, d))
  /** Pow each value of a raster by a double constant value.*/
  def **(d: Double): RasterSource = localPow(d)
  /** Pow a double constant value by each cell value.*/
  def localPowValue(d: Double): RasterSource = rasterSource.mapTile(Pow(d, _))
  /** Pow a double constant value by each cell value.*/
  def **:(d: Double): RasterSource = localPowValue(d)
  /** Pow the values of each cell in each raster. */
  def localPow(rs: RasterSource): RasterSource = rasterSource.combineTile(rs)(Pow(_, _))
  /** Pow the values of each cell in each raster. */
  def **(rs: RasterSource): RasterSource = localPow(rs)
  /** Pow the values of each cell in each raster. */
  def localPow(rss: Seq[RasterSource]): RasterSource = rasterSource.combineTile(rss)(Pow(_))
  /** Pow the values of each cell in each raster. */
  def **(rss: Seq[RasterSource]): RasterSource = localPow(rss)
}
