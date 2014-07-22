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

trait MultiplyOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int): RasterSource = self.mapTile(Multiply(_, i))
  /** Multiply a constant value from each cell.*/
  def *(i: Int): RasterSource = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i: Int): RasterSource = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double): RasterSource = self.mapTile(Multiply(_, d))
  /** Multiply a double constant value from each cell.*/
  def *(d: Double): RasterSource = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d: Double): RasterSource = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rs: RasterSource): RasterSource = self.combineTile(rs)(Multiply(_, _))
  /** Multiply the values of each cell in each raster. */
  def *(rs: RasterSource): RasterSource = localMultiply(rs)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rss: Seq[RasterSource]): RasterSource = self.combineTile(rss)(Multiply(_))
  /** Multiply the values of each cell in each raster. */
  def *(rss: Seq[RasterSource]): RasterSource = localMultiply(rss)
}
