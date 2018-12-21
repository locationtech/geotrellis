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

import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.{IndexRanges, KeyIndex, MergeQueue}
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

import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import spire.math.Interval
import spire.implicits._

import scala.collection.immutable.VectorBuilder

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
    keyIndex: KeyIndex[K],
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

    val query = instance.cassandraConfig.partitionStrategy match {
      case conf.`Read-Optimized-Partitioner` =>
        QueryBuilder.select("value")
          .from(keyspace, table)
          .where(eqs("name", layerId.name))
          .and(eqs("zoom", layerId.zoom))
          .and(eqs("zoombin", QueryBuilder.bindMarker()))
          .and(eqs("key", QueryBuilder.bindMarker()))
          .toString

      case conf.`Write-Optimized-Partitioner` =>
        QueryBuilder.select("value")
          .from(keyspace, table)
          .where(eqs("key", QueryBuilder.bindMarker()))
          .and(eqs("name", layerId.name))
          .and(eqs("zoom", layerId.zoom))
          .toString
    }

    lazy val intervals: Vector[(Interval[BigInt], Int)] = {
      val ranges = keyIndex.indexRanges(keyIndex.keyBounds)

      val binRanges = ranges.toVector.map{ range =>
        val vb = new VectorBuilder[Interval[BigInt]]()
        cfor(range._1)(_ <= range._2, _ + instance.cassandraConfig.tilesPerPartition){ i =>
          vb += Interval.openUpper(i, i + instance.cassandraConfig.tilesPerPartition)
        }
        vb.result()
      }

      binRanges.flatten.zipWithIndex
    }

    def zoomBin(key: BigInteger): java.lang.Integer = {
      intervals.find{ case (interval, idx) => interval.contains(key) }.map {
        _._2: java.lang.Integer
      }.getOrElse(0: java.lang.Integer)
    }

    def bindQuery(statement: PreparedStatement, index: BigInteger): BoundStatement = {
      instance.cassandraConfig.partitionStrategy match {
        case conf.`Read-Optimized-Partitioner` =>
          statement.bind(zoomBin(index), index)
        case conf.`Write-Optimized-Partitioner` =>
          statement.bind(index)
      }
    }

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        instance.withSession { session =>
          val statement = session.prepare(query)

          val result = partition map { seq =>
            LayerReader.njoin[K, V](seq.iterator, threads) { index: BigInt =>
              val row = session.execute(bindQuery(statement, index: BigInteger))
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
