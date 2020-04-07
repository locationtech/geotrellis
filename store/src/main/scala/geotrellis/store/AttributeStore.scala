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

package geotrellis.store

import geotrellis.store.cog.ZoomRange
import geotrellis.store.index._

import org.apache.avro.Schema
import io.circe._
import io.circe.syntax._
import cats.syntax.either._

import scala.reflect._
import java.net.URI
import java.util.ServiceLoader

trait AttributeStore extends AttributeCaching with LayerAttributeStore {
  def read[T: Decoder](layerId: LayerId, attributeName: String): T
  def readAll[T: Decoder](attributeName: String): Map[LayerId, T]
  def write[T: Encoder](layerId: LayerId, attributeName: String, value: T): Unit
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
      write(to, attribute, read[Json](from, attribute))
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
  def readHeader[H: Decoder](id: LayerId): H
  def readMetadata[M: Decoder](id: LayerId): M
  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K]
  def readSchema(id: LayerId): Schema
  def readLayerAttributes[H: Decoder, M: Decoder, K: ClassTag](id: LayerId): LayerAttributes[H, M, K]
  def writeLayerAttributes[H: Encoder, M: Encoder, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema): Unit
  def readCOGLayerAttributes[H: Decoder, M: Decoder](id: LayerId): COGLayerAttributes[H, M]
  def writeCOGLayerAttributes[H: Encoder, M: Encoder](id: LayerId, header: H, metadata: M): Unit
}


trait BlobLayerAttributeStore extends AttributeStore {
  import AttributeStore._

  def readHeader[H: Decoder](id: LayerId): H =
    cacheRead[Json](id, Fields.metadataBlob).hcursor.downField(Fields.header).as[H].valueOr(throw _)

  def readMetadata[M: Decoder](id: LayerId): M =
      cacheRead[Json](id, Fields.metadata).hcursor.downField(Fields.metadata).as[M].valueOr(throw _)

  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K] = {
    layerType(id) match {
      // TODO: Find a way to read a single KeyIndex from the COGMetadata
      case COGLayerType => throw new UnsupportedOperationException(s"The readKeyIndex cannot be performed on COGLayer: $id")
      case AvroLayerType =>
        cacheRead[Json](id, Fields.metadataBlob).hcursor.downField(AvroLayerFields.keyIndex).as[KeyIndex[K]].valueOr(throw _)
    }
  }

  def readKeyIndexes[K: ClassTag](id: LayerId): Map[ZoomRange, KeyIndex[K]] =
    layerType(id) match {
      case COGLayerType =>
        cacheRead[Json](id, Fields.metadataBlob).hcursor.downField(Fields.metadata).downField("keyIndexes").values match {
          case Some(keyIndexes) => keyIndexes.map { _.as[(ZoomRange, KeyIndex[K])].valueOr(throw _) }.toMap
          case _ => throw new AvroLayerAttributeError("keyIndexes", id)
        }
      case AvroLayerType => throw new AvroLayerAttributeError("keyIndexes", id)
    }

  def readSchema(id: LayerId): Schema =
    layerType(id) match {
      case COGLayerType => throw new COGLayerAttributeError("schema", id)
      case AvroLayerType =>
        cacheRead[Json](id, Fields.metadataBlob).hcursor.downField(AvroLayerFields.schema).as[Schema].valueOr(throw _)
    }

  def readLayerAttributes[H: Decoder, M: Decoder, K: ClassTag](id: LayerId): LayerAttributes[H, M, K] = {
    val blob = cacheRead[Json](id, Fields.metadataBlob).hcursor
    LayerAttributes(
      blob.downField(Fields.header).as[H].valueOr(throw _),
      blob.downField(Fields.metadata).as[M].valueOr(throw _),
      blob.downField(AvroLayerFields.keyIndex).as[KeyIndex[K]].valueOr(throw _),
      blob.downField(AvroLayerFields.schema).as[Schema].valueOr(throw _)
    )
  }

  def writeLayerAttributes[H: Encoder, M: Encoder, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema): Unit = {
    cacheWrite(id, Fields.metadataBlob,
      Json.obj(
        Fields.header -> header.asJson,
        Fields.metadata -> metadata.asJson,
        AvroLayerFields.keyIndex -> keyIndex.asJson,
        AvroLayerFields.schema -> schema.asJson
      )
    )
  }

  def readCOGLayerAttributes[H: Decoder, M: Decoder](id: LayerId): COGLayerAttributes[H, M] = {
    val blob = cacheRead[Json](id, Fields.metadataBlob).hcursor
    COGLayerAttributes(
      blob.downField(Fields.header).as[H].valueOr(throw _),
      blob.downField(Fields.metadata).as[M].valueOr(throw _)
    )
  }

  def writeCOGLayerAttributes[H: Encoder, M: Encoder](id: LayerId, header: H, metadata: M): Unit =
    cacheWrite(id, Fields.metadataBlob,
      Json.obj(
        Fields.header -> header.asJson,
        Fields.metadata -> metadata.asJson
      )
    )
}

trait DiscreteLayerAttributeStore extends AttributeStore {
  import AttributeStore._

  def readKeyIndexes[K: ClassTag](id: LayerId): Map[ZoomRange, KeyIndex[K]] =
    throw new AvroLayerAttributeError("keyIndexes", id)

  def readHeader[H: Decoder](id: LayerId): H =
    cacheRead[H](id, Fields.header)

  def readMetadata[M: Decoder](id: LayerId): M =
    cacheRead[M](id, Fields.metadata)

  def readKeyIndex[K: ClassTag](id: LayerId): KeyIndex[K] =
    cacheRead[KeyIndex[K]](id, AvroLayerFields.keyIndex)

  def readSchema(id: LayerId): Schema =
    cacheRead[Schema](id, AvroLayerFields.schema)

  def readLayerAttributes[H: Decoder, M: Decoder, K: ClassTag](id: LayerId): LayerAttributes[H, M, K] = {
    LayerAttributes(
      readHeader[H](id),
      readMetadata[M](id),
      readKeyIndex[K](id),
      readSchema(id)
    )
  }

  def writeLayerAttributes[H: Encoder, M: Encoder, K: ClassTag](id: LayerId, header: H, metadata: M, keyIndex: KeyIndex[K], schema: Schema) = {
    cacheWrite(id, Fields.header, header)
    cacheWrite(id, Fields.metadata, metadata)
    cacheWrite(id, AvroLayerFields.keyIndex, keyIndex)
    cacheWrite(id, AvroLayerFields.schema, schema)
  }

  def readCOGLayerAttributes[H: Decoder, M: Decoder](id: LayerId): COGLayerAttributes[H, M] =
    throw new AvroLayerAttributeError("COGLayerAttributes", id)

  def writeCOGLayerAttributes[H: Encoder, M: Encoder](id: LayerId, header: H, metadata: M): Unit =
    throw new AvroLayerAttributeError("COGLayerAttributes", id)
}
