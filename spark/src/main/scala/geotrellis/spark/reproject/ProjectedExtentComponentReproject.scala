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

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.spark.resample._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd._

import spire.math.Integral

import scala.reflect.ClassTag

object ProjectedExtentComponentReproject {


  /** Reproject the given RDD and modify the key with the new CRS and extent
    */
  def apply[
    K: Component[?, ProjectedExtent],
    V <: CellGrid[Int]: (? => TileReprojectMethods[V]),
    N: Integral
  ](
    rdd: RDD[(K, V)],
    destCrs: CRS,
    resampleTarget: Option[ResampleTarget[N]],
    resampleMethod: ResampleMethod = ResampleMethods.NearestNeighbor,
    errorThreshold: Double = 0.125
  ): RDD[(K, V)] =
    rdd.map { case (key, tile) =>
      val ProjectedExtent(extent, crs) = key.getComponent[ProjectedExtent]
      val Raster(newTile , newExtent) =
        tile.reproject(extent, crs, destCrs, resampleTarget)
      val newKey = key.setComponent(ProjectedExtent(newExtent, destCrs))
      (newKey, newTile)
    }
}
