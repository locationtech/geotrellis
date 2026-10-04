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

import geotrellis.spark.store.LayerWriter
import geotrellis.store.avro.{AvroRecordCodec, AvroEncoder}
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.util.Filesystem

import org.apache.spark.rdd.RDD
import org.apache.avro.Schema

import scala.reflect.ClassTag

object FileRDDWriter {
  private[file] def update[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    rootPath: String,
    keyPath: K => String,
    writerSchema: Option[Schema],
    mergeFunc: Option[(V, V) => V]
  ): Unit = {
    implicit val codec: KeyValueRecordCodec[K, V] = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    val pathsToTiles: RDD[(String, Iterable[(K, V)])] =
      // Call groupBy with numPartitions; if called without that argument or a partitioner,
      // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
      // on a key type that may no longer by valid for the key type of the resulting RDD.
      rdd.groupBy({ case (k, _) => keyPath(k) }, numPartitions = rdd.partitions.length)

    Filesystem.ensureDirectory(rootPath)

    pathsToTiles.foreach { case (path, rows) =>
      val updated = LayerWriter.updateRecords(mergeFunc, rows.toVector, existing = {
        if (Filesystem.exists(path)) {
          val inBytes = Filesystem.slurp(path)
          val schema = writerSchema.getOrElse(codec.schema)
          AvroEncoder.fromBinary[Vector[(K, V)]](schema, inBytes)
        } else Vector.empty
      })

      val outBytes: Array[Byte] = AvroEncoder.toBinary[Vector[(K, V)]](updated)
      Filesystem.writeBytes(path, outBytes)
    }
  }

  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    rootPath: String,
    keyPath: K => String
  ): Unit = update(rdd, rootPath, keyPath, None, None)
}
