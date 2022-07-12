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

import geotrellis.layer._
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.index.MergeQueue
import geotrellis.store.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.store.util.{IORuntimeTransient, IOUtils => GTIOUtils}

import cats.effect._
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Client
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils

class S3CollectionReader(
  s3Client: => S3Client = S3ClientProducer.get(),
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends Serializable {

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](
     bucket: String,
     keyPath: BigInt => String,
     queryKeyBounds: Seq[KeyBounds[K]],
     decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
     filterIndexOnly: Boolean,
     writerSchema: Option[Schema] = None
   ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val recordCodec = KeyValueRecordCodec[K, V]
    implicit val ioRuntime: unsafe.IORuntime = runtime

    GTIOUtils.parJoin[K, V](ranges.iterator) { index: BigInt =>
      try {
        val getRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(keyPath(index))
          .build()
        val s3obj = s3Client.getObject(getRequest)
        val bytes = IOUtils.toByteArray(s3obj)
        s3obj.close()
        val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(recordCodec.schema), bytes)(recordCodec)
        if (filterIndexOnly) recs
        else recs.filter { row => queryKeyBounds.includeKey(row._1) }
      } catch {
        case e: S3Exception if e.statusCode == 404 => Vector.empty
      }
    }: Seq[(K, V)]
  }
}
