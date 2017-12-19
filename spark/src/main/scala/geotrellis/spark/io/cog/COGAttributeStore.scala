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

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json.Implicits._

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._


trait COGAttributeStore extends COGAttributeCaching with BaseCOGAttributeStore {
  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T
  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T]
  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit
  def layerExists(layerId: LayerId): Boolean
  def delete(layerId: LayerId): Unit
  def delete(layerId: LayerId, attributeName: String): Unit
  def layerIds: Seq[LayerId]
  def availableAttributes(id: LayerId): Seq[String]

  def copy(from: LayerId, to: LayerId): Unit =
    copy(from, to, availableAttributes(from))

  def copy(from: LayerId, to: LayerId, attributes: Seq[String]): Unit = {
    for (attribute <- attributes) {
      write(to, attribute, read[JsValue](from, attribute))
    }
  }
}

object COGAttributeStore {
  // TODO: Move to LayerAttributeStore, and potentially rename that to
  // AvroLayerAttributeStore (these things are not needed for COGs)
  object Fields {
    val metadataBlob = "metadata"
    val header = "header"
    val keyIndex = "keyIndex"
    val metadata = "metadata"
  }
}

case class COGLayerAttributes[H, M, K](header: H, metadata: M, keyIndex: KeyIndex[K])

trait BaseCOGAttributeStore extends Serializable {
  def readHeader[H: JsonFormat](id: LayerId): H
  def readMetadata[M: JsonFormat](id: LayerId): M
  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K]
  def readLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId): COGLayerAttributes[H, M, K]
  def writeLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K]): Unit
}


trait COGLayerAttributeStore extends COGAttributeStore {
  import COGAttributeStore._
  import DefaultJsonProtocol._

  def readHeader[H: JsonFormat](id: LayerId): H =
    cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(Fields.header).convertTo[H]

  def readMetadata[M: JsonFormat](id: LayerId): M =
    cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(Fields.metadata).convertTo[M]

  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K] =
    cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(Fields.keyIndex).convertTo[KeyIndex[K]]

  def readLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId): COGLayerAttributes[H, M, K] = {
    val blob = cacheRead[JsValue](id, Fields.metadataBlob).asJsObject
    COGLayerAttributes(
      blob.fields(Fields.header).convertTo[H],
      blob.fields(Fields.metadata).convertTo[M],
      blob.fields(Fields.keyIndex).convertTo[KeyIndex[K]]
    )
  }

  def writeLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K]): Unit = {
    cacheWrite(id, Fields.metadataBlob,
      JsObject(
        Fields.header -> header.toJson,
        Fields.metadata -> metadata.toJson,
        Fields.keyIndex -> keyIndex.toJson
      )
    )
  }
}
