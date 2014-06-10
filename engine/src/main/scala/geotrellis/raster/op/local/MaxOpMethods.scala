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

trait MaxOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Max a constant Int value to each cell. */
  def localMax(i: Int): RasterSource = self.map(Max(_, i))
  /** Max a constant Double value to each cell. */
  def localMax(d: Double): RasterSource = self.map(Max(_, d))
  /** Max the values of each cell in each raster.  */
  def localMax(rs:RasterSource): RasterSource = self.combine(rs)(Max(_,_))
  /** Max the values of each cell in each raster.  */
  def localMax(rss:Seq[RasterSource]): RasterSource = self.combine(rss)(Max(_))
}
