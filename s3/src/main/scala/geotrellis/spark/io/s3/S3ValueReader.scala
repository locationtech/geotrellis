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

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.tiling.SpatialComponent
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.io.index._
import com.amazonaws.services.s3.model.AmazonS3Exception
import geotrellis.layers.LayerId
import geotrellis.layers.io.{OverzoomingValueReader, Reader}
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class S3ValueReader(
  val attributeStore: AttributeStore
) extends OverzoomingValueReader {

  def s3Client: S3Client = S3Client.DEFAULT

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[S3LayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)


    def read(key: K): V = {
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val path = s"${header.key}/${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      val is =
        try {
          s3Client.getObject(header.bucket, path).getObjectContent
        } catch {
          case e: AmazonS3Exception if e.getStatusCode == 404 =>
            throw new ValueNotFoundError(key, layerId)
        }

      val bytes = IOUtils.toByteArray(is)
      val recs = AvroEncoder.fromBinary(writerSchema, bytes)(KeyValueRecordCodec[K, V])

      recs
        .find { row => row._1 == key }
        .map { row => row._2 }
        .getOrElse(throw new ValueNotFoundError(key, layerId))
    }
  }
}

object S3ValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    attributeStore: AttributeStore,
    layerId: LayerId
  ): Reader[K, V] =
    new S3ValueReader(attributeStore).reader[K, V](layerId)

  def apply[K: AvroRecordCodec: JsonFormat: SpatialComponent: ClassTag, V <: CellGrid[Int]: AvroRecordCodec: ? => TileResampleMethods[V]](
    attributeStore: AttributeStore,
    layerId: LayerId,
    resampleMethod: ResampleMethod
  ): Reader[K, V] =
    new S3ValueReader(attributeStore).overzoomingReader[K, V](layerId, resampleMethod)

  def apply(bucket: String, root: String): S3ValueReader =
    new S3ValueReader(new S3AttributeStore(bucket, root))

  def apply(bucket: String): S3ValueReader =
    apply(bucket, "")
}
