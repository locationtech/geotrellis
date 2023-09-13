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

package geotrellis.store.file

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.util._
import geotrellis.store.avro.AvroRecordCodec
import geotrellis.store.index.Index
import geotrellis.util._

import io.circe.Decoder
import cats.effect._

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from a filesystem.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class FileCollectionLayerReader(
  val attributeStore: AttributeStore,
  catalogPath: String,
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends CollectionLayerReader[LayerId] {

  @transient implicit lazy val ioRuntime: unsafe.IORuntime = runtime

  def read[
    K: AvroRecordCodec: Boundable: Decoder: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Decoder: Component[*, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[FileLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path

    val queryKeyBounds = rasterQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, layerPath, maxWidth)
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val seq = FileCollectionReader.read[K, V](keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))

    new ContextCollection(seq, layerMetadata)
  }
}

object FileCollectionLayerReader {
  def apply(attributeStore: AttributeStore, catalogPath: String): FileCollectionLayerReader =
    new FileCollectionLayerReader(attributeStore, catalogPath)

  def apply(catalogPath: String): FileCollectionLayerReader =
    apply(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore): FileCollectionLayerReader =
    apply(attributeStore, attributeStore.catalogPath)
}
