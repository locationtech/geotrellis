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

trait AndOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** And a constant Int value to each cell. */
  def localAnd(i: Int): RasterSource = self.mapTile(And(_, i))
  /** And a constant Int value to each cell. */
  def &(i: Int): RasterSource = localAnd(i)
  /** And a constant Int value to each cell. */
  def &: (i: Int): RasterSource = localAnd(i)
  /** And the values of each cell in each raster.  */
  def localAnd(rs: RasterSource): RasterSource = self.combineTile(rs)(And(_, _))
  /** And the values of each cell in each raster. */
  def &(rs: RasterSource): RasterSource = localAnd(rs)
  /** And the values of each cell in each raster.  */
  def localAnd(rss: Seq[RasterSource]): RasterSource = self.combineTile(rss)(And(_))
  /** And the values of each cell in each raster. */
  def &(rss: Seq[RasterSource]): RasterSource = localAnd(rss)
}
