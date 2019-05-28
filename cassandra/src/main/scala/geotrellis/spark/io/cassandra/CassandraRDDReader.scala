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

package geotrellis.spark.io.cassandra

import geotrellis.tiling.{Boundable, KeyBounds}
import geotrellis.spark.io._
import geotrellis.layers.io.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.layers.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.cassandra.conf.CassandraConfig
import geotrellis.spark.util.KryoWrapper
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import java.math.BigInteger

import geotrellis.layers.LayerId

object CassandraRDDReader {
  final val defaultThreadCount = CassandraConfig.threads.rdd.readThreads

  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: CassandraInstance,
    keyspace: String,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None,
    threads: Int = defaultThreadCount
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if (queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    val query = QueryBuilder.select("value")
      .from(keyspace, table)
      .where(eqs("key", QueryBuilder.bindMarker()))
      .and(eqs("name", layerId.name))
      .and(eqs("zoom", layerId.zoom))
      .toString

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        instance.withSession { session =>
          val statement = session.prepare(query)

          val result = partition map { seq =>
            LayerReader.njoin[K, V](seq.iterator, threads) { index: BigInt =>
              val row = session.execute(statement.bind(index: BigInteger))
              if (row.asScala.nonEmpty) {
                val bytes = row.one().getBytes("value").array()
                val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                if (filterIndexOnly) recs
                else recs.filter { row => includeKey(row._1) }
              } else Vector.empty
            }
          }

          /** Close partition session */
          (result ++ Iterator({
            session.closeAsync(); session.getCluster.closeAsync(); Seq.empty[(K, V)]
          })).flatten
        }
      }
  }
}
