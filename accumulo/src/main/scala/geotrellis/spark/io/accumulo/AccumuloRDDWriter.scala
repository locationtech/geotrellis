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

package geotrellis.spark.io.accumulo

import geotrellis.layers.accumulo.AccumuloInstance
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.util.KryoWrapper

import org.apache.accumulo.core.data.{Key, Range, Value}
import org.apache.accumulo.core.security.Authorizations
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._

object AccumuloRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: AccumuloInstance,
    encodeKey: K => Key,
    writeStrategy: AccumuloWriteStrategy,
    table: String
  ): Unit = update(raster, instance, encodeKey, writeStrategy, table, None, None)

  private[accumulo] def update[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: AccumuloInstance,
    encodeKey: K => Key,
    writeStrategy: AccumuloWriteStrategy,
    table: String,
    writerSchema: Option[Schema],
    mergeFunc: Option[(V,V) => V]
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    instance.ensureTableExists(table)

    val kwWriterSchema = KryoWrapper(writerSchema)

    val kvPairs: RDD[(Key, Value)] =
      raster
        // Call groupBy with numPartitions; if called without that argument or a partitioner,
        // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
        // on a key type that may no longer by valid for the key type of the resulting RDD.
        .groupBy({ row => encodeKey(row._1) }, numPartitions = raster.partitions.length)
        .mapPartitions { it => 
          val scanner = instance.connector.createScanner(table, Authorizations.EMPTY)

          it.map { case (key, _kvs1) =>
            val current: Vector[(K,V)] = _kvs1.toVector
            val updated = LayerWriter.updateRecords(mergeFunc, current, existing = {
              scanner.setRange(new Range(key.getRow))
              scanner.fetchColumnFamily(key.getColumnFamily)
              scanner.iterator().asScala.toVector.flatMap({ entry =>
                val value = entry.getValue
                AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.schema), value.get)(codec)
              })
            })

            (key, new Value(AvroEncoder.toBinary(updated)(codec)))
          } ++ { // closing hook for when the iterator reaches the end
            scanner.close()
            Iterator.empty
          }
        }

    writeStrategy.write(kvPairs, instance, table)
  }
}
