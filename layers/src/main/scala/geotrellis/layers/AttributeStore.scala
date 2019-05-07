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

package geotrellis.layers

import geotrellis.layers.cog.ZoomRange
import geotrellis.layers.index._
import geotrellis.layers.json.Implicits._

import org.apache.avro.Schema

import spray.json._
import spray.json.DefaultJsonProtocol.JsValueFormat

import scala.reflect._
import java.net.URI
import java.util.ServiceLoader


trait AttributeStore extends AttributeCaching with LayerAttributeStore {
  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T
  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T]
  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit
  def layerExists(layerId: LayerId): Boolean
  def delete(layerId: LayerId): Unit
  def delete(layerId: LayerId, attributeName: String): Unit
  def layerIds: Seq[LayerId]
  def availableAttributes(id: LayerId): Seq[String]
  def readKeyIndexes[K: ClassTag](id: LayerId): Map[ZoomRange, KeyIndex[K]]

  def isCOGLayer(id: LayerId): Boolean = {
    layerType(id) match {
      case COGLayerType => true
      case _ => false
    }
  }

  def layerType(id: LayerId): LayerType = {
    lazy val layerType = readHeader[LayerHeader](id).layerType
    cacheLayerType(id, layerType)
  }

  /** Return a map with layer names and the list of available zoom levels for each.
   *
   * This function should be re-implemented by AttributeStore subclasses so that
   * catalogs with large numbers of layers can be queried efficiently.
   */
  def layersWithZoomLevels: Map[String, Seq[Int]] = layerIds.groupBy(_.name).mapValues(_.map(_.zoom))

  /** Return a sequence of available zoom levels for a named layer.
   *
   * This function should be re-implemented by AttributeStore subclasses so that
   * catalogs with large numbers of layers can be queried efficiently.
   */
  def availableZoomLevels(layerName: String): Seq[Int] = layersWithZoomLevels(layerName)

  def copy(from: LayerId, to: LayerId): Unit =
    copy(from, to, availableAttributes(from))

  def copy(from: LayerId, to: LayerId, attributes: Seq[String]): Unit = {
    for (attribute <- attributes) {
      write(to, attribute, read[JsValue](from, attribute))
    }
  }
}

object AttributeStore {
  // TODO: Move to LayerAttributeStore, and potentially rename that to
  // AvroLayerAttributeStore (these things are not needed for COGs)
  object Fields {
    val metadataBlob = "metadata"
    val header = "header"
    val metadata = "metadata"
  }

  object AvroLayerFields {
    val keyIndex = "keyIndex"
    val schema = "schema"
  }

  /**
   * Produce AttributeStore instance based on URI description.
   * This method uses instances of [[AttributeServiceProvider]] loaded through Java SPI.
   */
  def apply(uri: URI): AttributeStore = {
    import scala.collection.JavaConverters._

    ServiceLoader.load(classOf[AttributeStoreProvider])
      .iterator().asScala
      .find(_.canProcess(uri))
      .getOrElse(throw new RuntimeException(s"Unable to find AttributeStoreProvider for $uri"))
      .attributeStore(uri)
  }

  def apply(uri: String): AttributeStore = apply(new URI(uri))
}

case class LayerAttributes[H, M, K](header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema)
case class COGLayerAttributes[H, M](header: H, metadata: M)

trait LayerAttributeStore extends Serializable {
  def readHeader[H: JsonFormat](id: LayerId): H
  def readMetadata[M: JsonFormat](id: LayerId): M
  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K]
  def readSchema(id: LayerId): Schema
  def readLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId): LayerAttributes[H, M, K]
  def writeLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema): Unit
  def readCOGLayerAttributes[H: JsonFormat, M: JsonFormat](id: LayerId): COGLayerAttributes[H, M]
  def writeCOGLayerAttributes[H: JsonFormat, M: JsonFormat](id: LayerId, header: H, metadata: M): Unit
}


trait BlobLayerAttributeStore extends AttributeStore {
  import AttributeStore._
  import DefaultJsonProtocol._

  def readHeader[H: JsonFormat](id: LayerId): H =
    cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(Fields.header).convertTo[H]

  def readMetadata[M: JsonFormat](id: LayerId): M =
      cacheRead[JsValue](id, Fields.metadata).asJsObject.fields(Fields.metadata).convertTo[M]

  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K] = {
    layerType(id) match {
      // TODO: Find a way to read a single KeyIndex from the COGMetadata
      case COGLayerType => throw new UnsupportedOperationException(s"The readKeyIndex cannot be performed on COGLayer: $id")
      case AvroLayerType =>
        cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(AvroLayerFields.keyIndex).convertTo[KeyIndex[K]]
    }
  }

  def readKeyIndexes[K: ClassTag](id: LayerId): Map[ZoomRange, KeyIndex[K]] =
    layerType(id) match {
      case COGLayerType =>
        cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(Fields.metadata).asJsObject.fields("keyIndexes") match {
          case JsArray(keyIndexes) => keyIndexes.map { _.convertTo[(ZoomRange, KeyIndex[K])] }.toMap
          case _ => throw new AvroLayerAttributeError("keyIndexes", id)
        }
      case AvroLayerType => throw new AvroLayerAttributeError("keyIndexes", id)
    }

  def readSchema(id: LayerId): Schema =
    layerType(id) match {
      case COGLayerType => throw new COGLayerAttributeError("schema", id)
      case AvroLayerType =>
        cacheRead[JsValue](id, Fields.metadataBlob).asJsObject.fields(AvroLayerFields.schema).convertTo[Schema]
    }

  def readLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId): LayerAttributes[H, M, K] = {
    val blob = cacheRead[JsValue](id, Fields.metadataBlob).asJsObject
    LayerAttributes(
      blob.fields(Fields.header).convertTo[H],
      blob.fields(Fields.metadata).convertTo[M],
      blob.fields(AvroLayerFields.keyIndex).convertTo[KeyIndex[K]],
      blob.fields(AvroLayerFields.schema).convertTo[Schema]
    )
  }

  def writeLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema): Unit = {
    cacheWrite(id, Fields.metadataBlob,
      JsObject(
        Fields.header -> header.toJson,
        Fields.metadata -> metadata.toJson,
        AvroLayerFields.keyIndex -> keyIndex.toJson,
        AvroLayerFields.schema -> schema.toJson
      )
    )
  }

  def readCOGLayerAttributes[H: JsonFormat, M: JsonFormat](id: LayerId): COGLayerAttributes[H, M] = {
    val blob = cacheRead[JsValue](id, Fields.metadataBlob).asJsObject
    COGLayerAttributes(
      blob.fields(Fields.header).convertTo[H],
      blob.fields(Fields.metadata).convertTo[M]
    )
  }

  def writeCOGLayerAttributes[H: JsonFormat, M: JsonFormat](id: LayerId, header: H, metadata: M): Unit =
    cacheWrite(id, Fields.metadataBlob,
      JsObject(
        Fields.header -> header.toJson,
        Fields.metadata -> metadata.toJson
      )
    )
}

trait DiscreteLayerAttributeStore extends AttributeStore {
  import AttributeStore._

  def readKeyIndexes[K: ClassTag](id: LayerId): Map[ZoomRange, KeyIndex[K]] =
    throw new AvroLayerAttributeError("keyIndexes", id)

  def readHeader[H: JsonFormat](id: LayerId): H =
    cacheRead[H](id, Fields.header)

  def readMetadata[M: JsonFormat](id: LayerId): M =
    cacheRead[M](id, Fields.metadata)

  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K] =
    cacheRead[KeyIndex[K]](id, AvroLayerFields.keyIndex)

  def readSchema(id: LayerId): Schema =
    cacheRead[Schema](id, AvroLayerFields.schema)

  def readLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId): LayerAttributes[H, M, K] = {
    LayerAttributes(
      readHeader[H](id),
      readMetadata[M](id),
      readKeyIndex[K](id),
      readSchema(id)
    )
  }

  def writeLayerAttributes[H: JsonFormat, M: JsonFormat, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema) = {
    cacheWrite(id, Fields.header, header)
    cacheWrite(id, Fields.metadata, metadata)
    cacheWrite(id, AvroLayerFields.keyIndex, keyIndex)
    cacheWrite(id, AvroLayerFields.schema, schema)
  }

  def readCOGLayerAttributes[H: JsonFormat, M: JsonFormat](id: LayerId): COGLayerAttributes[H, M] =
    throw new AvroLayerAttributeError("COGLayerAttributes", id)

  def writeCOGLayerAttributes[H: JsonFormat, M: JsonFormat](id: LayerId, header: H, metadata: M): Unit =
    throw new AvroLayerAttributeError("COGLayerAttributes", id)
}
