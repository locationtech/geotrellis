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

trait MultiplyRasterSourceMethods extends RasterSourceMethods {
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int): RasterSource = rasterSource.mapTile(Multiply(_, i))
  /** Multiply a constant value from each cell.*/
  def *(i: Int): RasterSource = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i: Int): RasterSource = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double): RasterSource = rasterSource.mapTile(Multiply(_, d))
  /** Multiply a double constant value from each cell.*/
  def *(d: Double): RasterSource = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d: Double): RasterSource = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rs: RasterSource): RasterSource = rasterSource.combineTile(rs)(Multiply(_, _))
  /** Multiply the values of each cell in each raster. */
  def *(rs: RasterSource): RasterSource = localMultiply(rs)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rss: Seq[RasterSource]): RasterSource = rasterSource.combineTile(rss)(Multiply(_))
  /** Multiply the values of each cell in each raster. */
  def *(rss: Seq[RasterSource]): RasterSource = localMultiply(rss)
}
