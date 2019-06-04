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

package geotrellis.spark.store

import geotrellis.tiling._
import geotrellis.layers.Metadata
import geotrellis.layers.{LayerQuery, BoundLayerQuery}
import geotrellis.layers.avro._
import geotrellis.layers.json._
import geotrellis.spark._
import geotrellis.util._
import org.apache.spark.rdd._
import spray.json._

import scala.reflect._


abstract class FilteringLayerReader[ID] extends LayerReader[ID] {

  /** read
    *
    * This function will read an RDD layer based on a query.
    *
    * @param id              The ID of the layer to be read
    * @param rasterQuery     The query that will specify the filter for this read.
    * @param numPartitions   The desired number of partitions in the resulting RDD.
    * @param indexFilterOnly If true, the reader should only filter out elements who's KeyIndex entries
    *                        do not match the indexes of the query key bounds. This can include keys that
    *                        are not inside the query key bounds.
    * @tparam K              Type of RDD Key (ex: SpatialKey)
    * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
    * @tparam M              Type of Metadata associated with the RDD[(K,V)]

    */
  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], numPartitions: Int, indexFilterOnly: Boolean): RDD[(K, V)] with Metadata[M]

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], numPartitions: Int): RDD[(K, V)] with Metadata[M] =
    read(id, rasterQuery, numPartitions, false)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M]): RDD[(K, V)] with Metadata[M] =
    read(id, rasterQuery, defaultNumPartitions)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[M] =
    read(id, new LayerQuery[K, M], numPartitions)

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](layerId: ID): BoundLayerQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read(layerId, _))

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](layerId: ID, numPartitions: Int): BoundLayerQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read(layerId, _, numPartitions))
}
