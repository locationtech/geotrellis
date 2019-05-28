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

package geotrellis.spark.store.cassandra

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.store.cassandra._
import geotrellis.layers.index._
import geotrellis.spark.store._
import geotrellis.util._

import org.apache.spark.SparkContext

import spray.json.JsonFormat

import java.time.ZonedDateTime

import scala.reflect.ClassTag

object CassandraLayerReindexer {
  def apply(attributeStore: AttributeStore,
            layerReader : LayerReader[LayerId],
            layerWriter : LayerWriter[LayerId],
            layerDeleter: LayerDeleter[LayerId],
            layerCopier : LayerCopier[LayerId])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    GenericLayerReindexer[CassandraLayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)

  def apply(
    attributeStore: CassandraAttributeStore
  )(implicit sc: SparkContext): CassandraLayerReindexer =
    new CassandraLayerReindexer(attributeStore.instance, attributeStore)

  def apply(
    instance: CassandraInstance,
    attributeStore: AttributeStore
  )(implicit sc: SparkContext): CassandraLayerReindexer =
    new CassandraLayerReindexer(instance, attributeStore)

  def apply(
    instance: CassandraInstance
  )(implicit sc: SparkContext): CassandraLayerReindexer =
    apply(instance, CassandraAttributeStore(instance))
}

class CassandraLayerReindexer(
  instance: CassandraInstance,
  attributeStore: AttributeStore
)(implicit sc: SparkContext) extends LayerReindexer[LayerId] {

  def getTmpId(id: LayerId): LayerId =
    id.copy(name = s"${id.name}-${ZonedDateTime.now.toInstant.toEpochMilli}")

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

    val header = attributeStore.readHeader[CassandraLayerHeader](id)
    val (keyspace, table) = header.keyspace -> header.tileTable

    val layerReader = CassandraLayerReader(instance)
    val layerWriter = CassandraLayerWriter(instance, keyspace, table)
    val layerDeleter = CassandraLayerDeleter(instance)
    val layerCopier = CassandraLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndex)
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

    val header = attributeStore.readHeader[CassandraLayerHeader](id)
    val existingKeyIndex = attributeStore.readKeyIndex[K](id)

    val (keyspace, table) = header.keyspace -> header.tileTable

    val layerReader = CassandraLayerReader(instance)
    val layerWriter = CassandraLayerWriter(instance, keyspace, table)
    val layerDeleter = CassandraLayerDeleter(instance)
    val layerCopier = CassandraLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(existingKeyIndex.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}
