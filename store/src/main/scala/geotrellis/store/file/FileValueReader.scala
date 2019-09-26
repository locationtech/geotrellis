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

import geotrellis.store._
import geotrellis.store.avro._
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.index._
import geotrellis.layer._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.util.Filesystem

import _root_.io.circe._

import scala.reflect.ClassTag
import java.io.File

class FileValueReader(
  val attributeStore: AttributeStore,
  catalogPath: String
) extends OverzoomingValueReader {

  def reader[K: AvroRecordCodec: Decoder: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val header = attributeStore.readHeader[FileLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, header.path, keyIndex, maxWidth)

    def read(key: K): V = {
      val path = keyPath(key)

      if(!new File(path).exists)
        throw new ValueNotFoundError(key, layerId)

      val bytes = Filesystem.slurp(path)
      val recs = AvroEncoder.fromBinary(writerSchema, bytes)(KeyValueRecordCodec[K, V])

      recs
        .find { case (recordKey, _) => recordKey == key }
        .map { case (_, recordValue) => recordValue }
        .getOrElse(throw new ValueNotFoundError(key, layerId))
    }
  }
}

object FileValueReader {
  def apply[K: AvroRecordCodec: Decoder: ClassTag, V: AvroRecordCodec](
    attributeStore: AttributeStore,
    catalogPath: String,
    layerId: LayerId
  ): Reader[K, V] =
    new FileValueReader(attributeStore, catalogPath).reader(layerId)

  def apply[K: AvroRecordCodec: Decoder: SpatialComponent: ClassTag, V <: CellGrid[Int]: AvroRecordCodec: * => TileResampleMethods[V]](
    attributeStore: AttributeStore,
    catalogPath: String,
    layerId: LayerId,
    resampleMethod: ResampleMethod
  ): Reader[K, V] =
    new FileValueReader(attributeStore, catalogPath).overzoomingReader(layerId, resampleMethod)

  def apply(catalogPath: String): FileValueReader =
    new FileValueReader(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore): FileValueReader =
    new FileValueReader(attributeStore, attributeStore.catalogPath)
}
