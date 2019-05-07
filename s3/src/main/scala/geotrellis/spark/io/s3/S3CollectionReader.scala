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

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.io.index.MergeQueue
import geotrellis.layers.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.s3.conf.S3Config

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils

trait S3CollectionReader {

  def getS3Client: () => S3Client

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](
     bucket: String,
     keyPath: BigInt => String,
     queryKeyBounds: Seq[KeyBounds[K]],
     decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
     filterIndexOnly: Boolean,
     writerSchema: Option[Schema] = None,
     threads: Int = S3CollectionReader.defaultThreadCount
   ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val recordCodec = KeyValueRecordCodec[K, V]
    val s3client = getS3Client()

    LayerReader.njoin[K, V](ranges.toIterator, threads){ index: BigInt =>
      try {
        val bytes = IOUtils.toByteArray(s3client.getObject(bucket, keyPath(index)).getObjectContent)
        val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(recordCodec.schema), bytes)(recordCodec)
        if (filterIndexOnly) recs
        else recs.filter { row => queryKeyBounds.includeKey(row._1) }
      } catch {
        case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
      }
    }: Seq[(K, V)]
  }
}

object S3CollectionReader extends S3CollectionReader {
  val defaultThreadCount = S3Config.threads.collection.readThreads
  def getS3Client: () => S3Client = () => S3Client.DEFAULT
}
