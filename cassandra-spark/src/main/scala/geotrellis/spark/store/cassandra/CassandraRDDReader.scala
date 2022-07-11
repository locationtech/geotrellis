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

package geotrellis.spark.store.cassandra

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.cassandra._
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.store.index.{IndexRanges, MergeQueue}
import geotrellis.store.util.{IORuntimeTransient, IOUtils}
import geotrellis.spark.util.KryoWrapper

import cats.effect._
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import java.math.BigInteger

object CassandraRDDReader {
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
    runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
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

    val query = QueryBuilder
      .selectFrom(keyspace, table)
      .column("value")
      .whereColumn("key").isEqualTo(QueryBuilder.bindMarker())
      .whereColumn("name").isEqualTo(literal(layerId.name))
      .whereColumn("zoom").isEqualTo(literal(layerId.zoom))
      .asCql()

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        instance.withSession { session =>
          implicit val ioRuntime: unsafe.IORuntime = runtime
          val statement = session.prepare(query)

          val result = partition map { seq =>
            IOUtils.parJoinIO[K, V](seq.iterator) { index: BigInt =>
              session.executeF[IO](statement.bind(index.asJava)).map { row =>
                if (row.nonEmpty) {
                  val bytes = row.one().getByteBuffer("value").array()
                  val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                  if (filterIndexOnly) recs
                  else recs.filter { row => includeKey(row._1) }
                } else Vector.empty
              }
            }
          }

          /** Close partition session */
          (result ++ Iterator({ session.closeAsync(); Seq.empty[(K, V)] })).flatten
        }
      }
  }
}
