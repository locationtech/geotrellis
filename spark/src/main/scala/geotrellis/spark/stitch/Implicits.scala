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

package geotrellis.spark.stitch

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.tiling._
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.util._
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withSpatialTileLayoutRDDMethods[
    V <: CellGrid[Int]: Stitcher,
    M: GetComponent[?, LayoutDefinition]
  ](
    val self: RDD[(SpatialKey, V)] with Metadata[M]
  ) extends SpatialTileLayoutRDDStitchMethods[V, M]

  implicit class withSpatialTileRDDMethods[V <: CellGrid[Int]: Stitcher](
    val self: RDD[(SpatialKey, V)]
  ) extends SpatialTileRDDStitchMethods[V]

  implicit class withSpatialTileLayoutCollectionMethods[
    V <: CellGrid[Int]: Stitcher,
    M: GetComponent[?, LayoutDefinition]
  ](
    val self: Seq[(SpatialKey, V)] with Metadata[M]
  ) extends SpatialTileLayoutCollectionStitchMethods[V, M]

  implicit class withSpatialTileCollectionMethods[V <: CellGrid[Int]: Stitcher](
    val self: Seq[(SpatialKey, V)]
  ) extends SpatialTileCollectionStitchMethods[V]
}
