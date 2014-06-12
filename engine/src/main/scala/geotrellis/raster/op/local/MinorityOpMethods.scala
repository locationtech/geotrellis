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

import spire.syntax.cfor._
import scala.collection.mutable

trait MinorityOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rss: Seq[RasterSource]): RasterSource = 
    combine(rss, "Minority")(Minority(_))

  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rss: RasterSource*)(implicit d: DI): RasterSource = 
    localMinority(rss)

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n: Int, rss: Seq[RasterSource]): RasterSource = 
    combine(rss, "Minority")(Minority(n, _))

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n: Int, rss: RasterSource*)(implicit d: DI): RasterSource = 
    localMinority(n, rss)
}
