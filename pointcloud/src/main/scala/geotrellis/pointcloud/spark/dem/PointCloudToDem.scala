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

package geotrellis.pointcloud.spark.dem

import io.pdal._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._

import org.apache.spark.rdd.RDD

object PointCloudToDem {
  def apply[M: GetComponent[?, LayoutDefinition]](rdd: RDD[(SpatialKey, PointCloud)] with Metadata[M], tileDimensions: (Int, Int), options: PointToGrid.Options): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] =
    apply[M](rdd, options) { e => RasterExtent(e, tileDimensions._1, tileDimensions._2) }

  def apply[M: GetComponent[?, LayoutDefinition]](rdd: RDD[(SpatialKey, PointCloud)] with Metadata[M], cellSize: CellSize, options: PointToGrid.Options): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] =
   apply[M](rdd, options) { e => RasterExtent(e, cellSize) }

  def apply[M: GetComponent[?, LayoutDefinition]](rdd: RDD[(SpatialKey, PointCloud)] with Metadata[M], options: PointToGrid.Options)(createRE: Extent => RasterExtent): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val layoutDefinition = rdd.metadata.getComponent[LayoutDefinition]
    val mapTransform = layoutDefinition.mapTransform

    val result =
      rdd
        .collectNeighbors
        .mapPartitions({ partition =>
          partition.map { case (key, neighbors) =>
            val extent = mapTransform(key)
            val raster =
              PointToGrid.createRaster(neighbors.map(_._2._2), createRE(extent), options)
            (key, raster.tile)
          }
        }, preservesPartitioning = true)

    ContextRDD(result, layoutDefinition)
  }
}
