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

import spire.syntax.cfor._
import scala.collection.mutable

trait MajorityRasterSourceMethods extends RasterSourceMethods {

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rss: Seq[RasterSource]): RasterSource =
    rasterSource.combineTile(rss, "Majority")(Majority(_))

  /** Assigns to each cell the value within the given rasters that is the most numerous. */
  def localMajority(rss: RasterSource* )(implicit d: DI): RasterSource =
    localMajority(rss)

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rss: Seq[RasterSource]): RasterSource =
    rasterSource.combineTile(rss, "Majority")(Majority(n, _))

  /** Assigns to each cell the value within the given rasters that is the nth most numerous. */
  def localMajority(n: Int, rss: RasterSource* )(implicit d: DI): RasterSource =
    localMajority(n, rss)
}
