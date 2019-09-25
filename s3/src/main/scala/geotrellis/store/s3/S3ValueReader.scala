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

package geotrellis.store.s3

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.avro._
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.index._

import software.amazon.awssdk.services.s3.model.{S3Exception, GetObjectRequest}
import software.amazon.awssdk.services.s3.S3Client
import org.apache.commons.io.IOUtils
import _root_.io.circe._

import scala.reflect.ClassTag

class S3ValueReader(
  val attributeStore: AttributeStore,
  s3Client: => S3Client
) extends OverzoomingValueReader {

  def reader[K: AvroRecordCodec: Decoder: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[S3LayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)


    def read(key: K): V = {
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val path = s"${header.key}/${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      val is =
        try {
          val request = GetObjectRequest.builder()
            .bucket(header.bucket)
            .key(path)
            .build()

          s3Client.getObject(request)
        } catch {
          case e: S3Exception if e.statusCode == 404 =>
            throw new ValueNotFoundError(key, layerId)
        }

      val bytes = IOUtils.toByteArray(is)
      is.close()
      val recs = AvroEncoder.fromBinary(writerSchema, bytes)(KeyValueRecordCodec[K, V])

      recs
        .find { row => row._1 == key }
        .map { row => row._2 }
        .getOrElse(throw new ValueNotFoundError(key, layerId))
    }
  }
}

object S3ValueReader {
  def apply[K: AvroRecordCodec: Decoder: ClassTag, V: AvroRecordCodec](
    attributeStore: AttributeStore,
    layerId: LayerId,
    s3Client: => S3Client
  ): Reader[K, V] =
    new S3ValueReader(attributeStore, s3Client).reader[K, V](layerId)

  def apply[K: AvroRecordCodec: Decoder: SpatialComponent: ClassTag, V <: CellGrid[Int]: AvroRecordCodec: * => TileResampleMethods[V]](
    attributeStore: AttributeStore,
    layerId: LayerId,
    resampleMethod: ResampleMethod,
    s3Client: => S3Client
  ): Reader[K, V] =
    new S3ValueReader(attributeStore, s3Client).overzoomingReader[K, V](layerId, resampleMethod)

  def apply(bucket: String, root: String, s3Client: => S3Client): S3ValueReader = {
    val attStore = new S3AttributeStore(bucket, root, s3Client)
    new S3ValueReader(attStore, s3Client)
  }

  def apply(bucket: String, s3Client: => S3Client): S3ValueReader =
    apply(bucket, "", s3Client)
}
