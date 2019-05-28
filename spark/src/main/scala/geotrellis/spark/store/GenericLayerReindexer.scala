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

package geotrellis.spark.io

import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.index._
import geotrellis.layers.json._
import geotrellis.spark._
import geotrellis.util._
import org.apache.avro._
import spray.json._
import java.time.ZonedDateTime

import geotrellis.layers.LayerId

import scala.reflect.ClassTag

abstract class GenericLayerReindexer[Header:JsonFormat](
  attributeStore: AttributeStore,
  layerReader: LayerReader[LayerId],
  layerWriter: LayerWriter[LayerId],
  layerDeleter: LayerDeleter[LayerId],
  layerCopier: LayerCopier[LayerId]
) extends LayerReindexer[LayerId] {

  def getTmpId(id: LayerId): LayerId

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

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

    val index = attributeStore.readKeyIndex[K](id)
    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(index.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}

object GenericLayerReindexer {
  def apply[Header: JsonFormat](
    attributeStore: AttributeStore,
    layerReader: LayerReader[LayerId],
    layerWriter: LayerWriter[LayerId],
    layerDeleter: LayerDeleter[LayerId],
    layerCopier: LayerCopier[LayerId]
  ): LayerReindexer[LayerId] =
    new GenericLayerReindexer[Header](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier) {
      def getTmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-${ZonedDateTime.now.toInstant.toEpochMilli}")
    }
}
