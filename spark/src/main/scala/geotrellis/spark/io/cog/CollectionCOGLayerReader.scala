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
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util._
import spray.json._

import scala.reflect._

abstract class CollectionCOGLayerReader[ID] { self =>
  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[M]

  def reader[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ]: Reader[ID, Seq[(K, V)] with Metadata[M]] =
    new Reader[ID, Seq[(K, V)] with Metadata[M]] {
      def read(id: ID): Seq[(K, V)] with Metadata[M] =
        self.read[K, V, M](id)
    }

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M]): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, rasterQuery, false)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, new LayerQuery[K, M])

  def query[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](layerId: ID): BoundLayerQuery[K, M, Seq[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read[K, V, M](layerId, _))
}
