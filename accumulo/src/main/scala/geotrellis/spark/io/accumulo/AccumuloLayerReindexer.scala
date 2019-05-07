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

package geotrellis.spark.io.accumulo

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.index._
import geotrellis.layers.io.json._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import java.time.ZonedDateTime

import geotrellis.layers.LayerId

import scala.reflect.ClassTag

object AccumuloLayerReindexer {
  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerReindexer =
    new AccumuloLayerReindexer(instance, attributeStore, options)

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerReindexer =
    apply(instance, AccumuloAttributeStore(instance), options)

  def apply(
    instance: AccumuloInstance
  )(implicit sc: SparkContext): AccumuloLayerReindexer =
    apply(instance, AccumuloLayerWriter.Options.DEFAULT)
}

class AccumuloLayerReindexer(
  instance: AccumuloInstance,
  attributeStore: AttributeStore,
  options: AccumuloLayerWriter.Options
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

    val header = attributeStore.readHeader[AccumuloLayerHeader](id)
    val table = header.tileTable

    val layerReader = AccumuloLayerReader(instance)
    val layerWriter = AccumuloLayerWriter(instance, table, options)
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerCopier = AccumuloLayerCopier(attributeStore, layerReader, layerWriter)

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

    val header = attributeStore.readHeader[AccumuloLayerHeader](id)
    val existingKeyIndex = attributeStore.readKeyIndex[K](id)

    val table = header.tileTable

    val layerReader = AccumuloLayerReader(instance)
    val layerWriter = AccumuloLayerWriter(instance, table, options)
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerCopier = AccumuloLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(existingKeyIndex.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}
