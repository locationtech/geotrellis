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

package geotrellis.spark.io.s3

import geotrellis.layers.LayerId
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.layers.io.avro._
import geotrellis.layers.io.index._
import geotrellis.layers.io.json._
import geotrellis.util._

import software.amazon.awssdk.services.s3.S3Client

import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class S3LayerManager(attributeStore: S3AttributeStore, getClient: () => S3Client = S3ClientProducer.get)(implicit sc: SparkContext)
    extends LayerManager[LayerId] {
  def delete(id: LayerId): Unit =
    S3LayerDeleter(attributeStore, getClient).delete(id)

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    S3LayerCopier(attributeStore).copy[K, V, M](from, to)

  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit =
    S3LayerMover(attributeStore).move[K, V, M](from, to)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit =
    S3LayerReindexer(attributeStore).reindex[K, V, M](id, keyIndexMethod)

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit =
    S3LayerReindexer(attributeStore).reindex[K, V, M](id, keyIndex)
}
