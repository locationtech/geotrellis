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

package geotrellis.spark.io.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util._
import spray.json._

import scala.reflect._

abstract class COGCollectionLayerReader[ID] { self =>
  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: ID, rasterQuery: LayerQuery[K, TileLayerMetadata[K]], indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[TileLayerMetadata[K]]

  def reader[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ]: Reader[ID, Seq[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new Reader[ID, Seq[(K, V)] with Metadata[TileLayerMetadata[K]]] {
      def read(id: ID): Seq[(K, V)] with Metadata[TileLayerMetadata[K]] =
        self.read[K, V](id)
    }

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: ID, rasterQuery: LayerQuery[K, TileLayerMetadata[K]]): Seq[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read[K, V](id, rasterQuery, false)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: ID): Seq[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read[K, V](id, new LayerQuery[K, TileLayerMetadata[K]])

  def query[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](layerId: ID): BoundLayerQuery[K, TileLayerMetadata[K], Seq[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new BoundLayerQuery(new LayerQuery, read[K, V](layerId, _))
}
