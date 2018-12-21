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
import geotrellis.spark.io.cassandra.conf.CassandraConfig
import geotrellis.spark.io.index.{KeyIndex, MergeQueue}
import geotrellis.spark.util.KryoWrapper
import org.apache.avro.Schema
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import java.math.BigInteger

import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import spire.math.Interval
import spire.implicits._

import scala.collection.immutable.VectorBuilder

object CassandraCollectionReader {
  final val defaultThreadCount = CassandraConfig.threads.collection.readThreads

  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: CassandraInstance,
    keyspace: String,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    keyIndex: KeyIndex[K],
    threads: Int = defaultThreadCount
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

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

    instance.withSessionDo { session =>
      val statement = session.prepare(query)

      LayerReader.njoin[K, V](ranges.toIterator, threads){ index: BigInt =>
        val row = session.execute(bindQuery(statement, index: BigInteger))
        if (row.asScala.nonEmpty) {
          val bytes = row.one().getBytes("value").array()
          val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          if (filterIndexOnly) recs
          else recs.filter { row => includeKey(row._1) }
        } else Vector.empty
      }
    }: Seq[(K, V)]
  }
}
