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

import geotrellis.tiling._
import geotrellis.layers.avro._
import geotrellis.layers.json._
import geotrellis.util._

import spray.json._

import java.net.URI
import java.util.ServiceLoader

import scala.reflect._


abstract class CollectionLayerReader[ID] { self =>
  val attributeStore: AttributeStore

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[M]

  def reader[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ]: Reader[ID, Seq[(K, V)] with Metadata[M]] =
    new Reader[ID, Seq[(K, V)] with Metadata[M]] {
      def read(id: ID): Seq[(K, V)] with Metadata[M] =
        self.read[K, V, M](id)
    }

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M]): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, rasterQuery, false)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, new LayerQuery[K, M])

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](layerId: ID): BoundLayerQuery[K, M, Seq[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read[K, V, M](layerId, _))
}

object CollectionLayerReader {
  def apply(attributeStore: AttributeStore, collectionReaderUri: URI): CollectionLayerReader[LayerId] = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[CollectionLayerReaderProvider])
      .iterator().asScala
      .find(_.canProcess(collectionReaderUri))
      .getOrElse(throw new RuntimeException(s"Unable to find CollectionLayerReaderProvider for $collectionReaderUri"))
      .collectionLayerReader(collectionReaderUri, attributeStore)
  }

  def apply(attributeStoreUri: URI, collectionReaderUri: URI): CollectionLayerReader[LayerId] =
    apply(AttributeStore(attributeStoreUri), collectionReaderUri)

  def apply(uri: URI): CollectionLayerReader[LayerId] =
    apply(attributeStoreUri = uri, collectionReaderUri = uri)

  def apply(attributeStore: AttributeStore, collectionReaderUri: String): CollectionLayerReader[LayerId] =
    apply(attributeStore, new URI(collectionReaderUri))

  def apply(attributeStoreUri: String, collectionReaderUri: String): CollectionLayerReader[LayerId] =
    apply(AttributeStore(new URI(attributeStoreUri)), new URI(collectionReaderUri))

  def apply(uri: String): CollectionLayerReader[LayerId] = {
    val _uri = new URI(uri)
    apply(attributeStoreUri = _uri, collectionReaderUri = _uri)
  }
}
