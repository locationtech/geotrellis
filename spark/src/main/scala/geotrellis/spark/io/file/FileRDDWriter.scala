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

package geotrellis.spark.io.file

import geotrellis.spark.io.avro.{AvroRecordCodec, AvroEncoder}
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.util.Filesystem

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import java.io.File

object FileRDDWriter {
  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    rootPath: String,
    keyPath: K => String
  ): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    val pathsToTiles =
      // Call groupBy with numPartitions; if called without that argument or a partitioner,
      // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
      // on a key type that may no longer by valid for the key type of the resulting RDD.
      rdd.groupBy({ row => keyPath(row._1) }, numPartitions = rdd.partitions.length)

    Filesystem.ensureDirectory(rootPath)

    pathsToTiles.foreach { case (path, rows) =>
      val bytes = AvroEncoder.toBinary(rows.toVector)(codec)
      Filesystem.writeBytes(path, bytes)
    }
  }
}
