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

import geotrellis.tiling.{Boundable, Bounds, KeyBounds}
import geotrellis.spark._
import geotrellis.layers.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.index.{IndexRanges, MergeQueue}
import geotrellis.layers.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.layers.util.IOUtils
import geotrellis.spark.util.KryoWrapper
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.io.File

import geotrellis.layers.file.conf.FileConfig

object FileRDDReader {
  val defaultThreadCount: Int = FileConfig.threads.rdd.readThreads

  def read[K: AvroRecordCodec: Boundable, V: AvroRecordCodec](
    keyPath: BigInt => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None,
    threads: Int = defaultThreadCount
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
        partition flatMap { seq =>
          IOUtils.parJoin[K, V](seq.toIterator, threads) { index: BigInt =>
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
