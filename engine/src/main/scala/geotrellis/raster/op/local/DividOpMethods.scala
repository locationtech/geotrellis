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

trait DivideOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int): RasterSource = self.map(Divide(_, i))
  /** Divide each value of the raster by a constant value.*/
  def /(i: Int) = localDivide(i)
  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int): RasterSource = self.map(Divide(i, _))
  /** Divide a constant value by each cell value.*/
  def /: (i: Int): RasterSource = localDivideValue(i)
  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double): RasterSource = self.map(Divide(_, d))
  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double): RasterSource = localDivide(d)
  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double): RasterSource = self.map(Divide(d, _))
  /** Divide a double constant value by each cell value.*/
  def /: (d: Double): RasterSource = localDivideValue(d)
  /** Divide the values of each cell in each raster. */
  def localDivide(rs: RasterSource): RasterSource = self.combine(rs)(Divide(_, _))
  /** Divide the values of each cell in each raster. */
  def /(rs: RasterSource): RasterSource = localDivide(rs)
  /** Divide the values of each cell in each raster. */
  def localDivide(rss: Seq[RasterSource]): RasterSource = self.combine(rss)(Divide(_))
  /** Divide the values of each cell in each raster. */
  def /(rss: Seq[RasterSource]): RasterSource = localDivide(rss)
}
