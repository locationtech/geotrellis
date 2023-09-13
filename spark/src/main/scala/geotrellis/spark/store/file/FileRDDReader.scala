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

package geotrellis.spark.store.file

import geotrellis.layer.{Boundable, KeyBounds}
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.index.{IndexRanges, MergeQueue}
import geotrellis.store.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.store.util.{IORuntimeTransient, IOUtils}
import geotrellis.spark.util.KryoWrapper
import geotrellis.util.Filesystem

import cats.effect._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import java.io.File

object FileRDDReader {
  def read[K: AvroRecordCodec: Boundable, V: AvroRecordCodec](
    keyPath: BigInt => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None,
    runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if(queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) // Avro Schema is not Serializable

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        implicit val ioRuntime: unsafe.IORuntime = runtime

        partition flatMap { seq =>
          IOUtils.parJoin[K, V](seq.iterator) { index: BigInt =>
            val path = keyPath(index)
            if (new File(path).exists) {
              val bytes: Array[Byte] = Filesystem.slurp(path)
              val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
              if (filterIndexOnly) recs
              else recs.filter { row => includeKey(row._1) }
            } else Vector.empty
          }
        }
      }
  }
}
