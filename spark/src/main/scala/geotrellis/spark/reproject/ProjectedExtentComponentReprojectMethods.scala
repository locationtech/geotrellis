/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.util._

import org.apache.spark.rdd._

class ProjectedExtentComponentReprojectMethods[K: Component[?, ProjectedExtent], V <: CellGrid: (? => TileReprojectMethods[V])](val self: RDD[(K, V)])
    extends MethodExtensions[RDD[(K, V)]] {
  import geotrellis.raster.reproject.Reproject.Options

  def reproject(destCrs: CRS, options: Options): RDD[(K, V)] = {
    ProjectedExtentComponentReproject(self, destCrs, options)
  }

  def reproject(destCrs: CRS): RDD[(K, V)] =
    reproject(destCrs, Options.DEFAULT)
}
