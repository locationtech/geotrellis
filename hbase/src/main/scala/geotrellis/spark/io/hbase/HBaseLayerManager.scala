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

package geotrellis.spark.io.hbase

import geotrellis.layers.LayerId
import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro.AvroRecordCodec
import geotrellis.layers.io.index._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HBaseLayerManager(attributeStore: HBaseAttributeStore, instance: HBaseInstance)(implicit sc: SparkContext)
    extends LayerManager[LayerId]{
  def delete(id: LayerId): Unit =
    HBaseLayerDeleter(attributeStore, instance).delete(id)

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    HBaseLayerCopier(instance).copy[K, V, M](from, to)

  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    HBaseLayerMover(instance).move[K, V, M](from, to)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit =
    HBaseLayerReindexer(instance).reindex[K, V, M](id, keyIndexMethod)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit =
    HBaseLayerReindexer(instance).reindex[K, V, M](id, keyIndex)
}
