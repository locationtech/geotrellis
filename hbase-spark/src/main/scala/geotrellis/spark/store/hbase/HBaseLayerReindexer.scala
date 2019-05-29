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

package geotrellis.spark.store.hbase

import geotrellis.store.hbase._
import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.index._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag
import java.time.ZonedDateTime

import geotrellis.layers.LayerId

object HBaseLayerReindexer {
  def apply(attributeStore: AttributeStore,
            layerReader : LayerReader[LayerId],
            layerWriter : LayerWriter[LayerId],
            layerDeleter: LayerDeleter[LayerId],
            layerCopier : LayerCopier[LayerId])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    GenericLayerReindexer[HBaseLayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)

  def apply(
    attributeStore: HBaseAttributeStore
  )(implicit sc: SparkContext): HBaseLayerReindexer =
    new HBaseLayerReindexer(attributeStore.instance, attributeStore)

  def apply(
    instance: HBaseInstance,
    attributeStore: AttributeStore
  )(implicit sc: SparkContext): HBaseLayerReindexer =
    new HBaseLayerReindexer(instance, attributeStore)

  def apply(
    instance: HBaseInstance
  )(implicit sc: SparkContext): HBaseLayerReindexer =
    apply(instance, HBaseAttributeStore(instance))
}

class HBaseLayerReindexer(
  instance: HBaseInstance,
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

    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable

    val layerReader = HBaseLayerReader(instance)
    val layerWriter = HBaseLayerWriter(instance, table)
    val layerDeleter = HBaseLayerDeleter(instance)
    val layerCopier = HBaseLayerCopier(attributeStore, layerReader, layerWriter)

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

    val existingKeyIndex = attributeStore.readKeyIndex[K](id)

    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable

    val layerReader = HBaseLayerReader(instance)
    val layerWriter = HBaseLayerWriter(instance, table)
    val layerDeleter = HBaseLayerDeleter(instance)
    val layerCopier = HBaseLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(existingKeyIndex.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}
