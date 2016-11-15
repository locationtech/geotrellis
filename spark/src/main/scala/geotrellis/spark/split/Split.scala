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

package geotrellis.spark.split

import geotrellis.raster._
import geotrellis.raster.split._
import geotrellis.raster.split.Split.Options
import geotrellis.spark._
import geotrellis.vector.ProjectedExtent
import geotrellis.util._

import org.apache.spark.rdd.RDD

object Split {
  /** Splits an RDD of tiles into tiles of size (tileCols x tileRows), and updates the ProjectedExtent component of the keys.
    */
  def apply[K: Component[?, ProjectedExtent], V <: CellGrid: (? => SplitMethods[V])](rdd: RDD[(K, V)], tileCols: Int, tileRows: Int): RDD[(K, V)] =
    rdd
      .flatMap { case (key, tile) =>
        val splitLayout =
          TileLayout(
            math.ceil(tile.cols / tileCols.toDouble).toInt,
            math.ceil(tile.rows / tileRows.toDouble).toInt,
            tileCols,
            tileRows
          )

        if(!splitLayout.isTiled) {
          Array((key, tile))
        } else {
          val ProjectedExtent(extent, crs) = key.getComponent[ProjectedExtent]
          Raster(tile, extent).split(splitLayout, Options(extend = false, cropped = false))
            .map { raster => (key.setComponent(ProjectedExtent(raster.extent, crs)), raster.tile) }
        }
      }
}
