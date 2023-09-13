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

import geotrellis.store._
import geotrellis.store.avro._
import geotrellis.store.avro.codecs._
import geotrellis.store.cassandra._
import geotrellis.spark.store._
import geotrellis.spark.util.KryoWrapper
import geotrellis.store.util.IORuntimeTransient

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.api.core.`type`.DataTypes
import cats.effect._
import cats.syntax.either._
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD

import java.nio.ByteBuffer
import java.math.BigInteger

object CassandraRDDWriter {
  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    rdd: RDD[(K, V)],
    instance: CassandraInstance,
    layerId: LayerId,
    decomposeKey: K => BigInt,
    keyspace: String,
    table: String,
    runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
  ): Unit = update(rdd, instance, layerId, decomposeKey, keyspace, table, None, None, runtime)

  private[cassandra] def update[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: CassandraInstance,
    layerId: LayerId,
    decomposeKey: K => BigInt,
    keyspace: String,
    table: String,
    writerSchema: Option[Schema],
    mergeFunc: Option[(V,V) => V],
    runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec = KeyValueRecordCodec[K, V]

    instance.withSessionDo { session =>
      instance.ensureKeyspaceExists(keyspace, session)
      session.execute(
        SchemaBuilder.createTable(keyspace, table).ifNotExists()
          .withPartitionKey("key", DataTypes.VARINT)
          .withClusteringColumn("name", DataTypes.TEXT)
          .withClusteringColumn("zoom", DataTypes.INT)
          .withColumn("value", DataTypes.BLOB)
          .build()
      )
    }

    val readQuery =
      QueryBuilder.selectFrom(keyspace, table)
        .column("value")
        .whereColumn("key").isEqualTo(QueryBuilder.bindMarker())
        .whereColumn("name").isEqualTo(literal(layerId.name))
        .whereColumn("zoom").isEqualTo(literal(layerId.zoom))
        .asCql()


    val writeQuery =
      QueryBuilder
        .insertInto(keyspace, table)
        .value("name", literal(layerId.name))
        .value("zoom", literal(layerId.zoom))
        .value("key", QueryBuilder.bindMarker())
        .value("value", QueryBuilder.bindMarker())
        .asCql()

    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema)

    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.
      raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .foreachPartition { partition: Iterator[(BigInt, Iterable[(K, V)])] =>
          if(partition.nonEmpty) {
            instance.withSession { session =>
              val readStatement = session.prepare(readQuery)
              val writeStatement = session.prepare(writeQuery)

              val rows: fs2.Stream[IO, (BigInt, Vector[(K,V)])] =
                fs2.Stream.fromIterator[IO](
                  partition.map { case (key, value) => (key, value.toVector) }, chunkSize = 1
                )

              implicit val ioRuntime: unsafe.IORuntime = runtime

              def elaborateRow(row: (BigInt, Vector[(K,V)])): fs2.Stream[IO, (BigInt, Vector[(K,V)])] = {
                fs2.Stream eval {
                  val (key, current) = row
                  val updated = LayerWriter.updateRecordsM(mergeFunc, current, existing = {
                    session.executeF[IO](readStatement.bind(key.asJava)).map { oldRow =>
                      if (oldRow.nonEmpty) {
                        val bytes = oldRow.one().getByteBuffer("value").array()
                        val schema = kwWriterSchema.value.getOrElse(_recordCodec.schema)
                        AvroEncoder.fromBinary(schema, bytes)(_recordCodec)
                      } else Vector.empty
                    }
                  })

                  updated.map(key -> _)
                }
              }

              def rowToBytes(row: (BigInt, Vector[(K,V)])): fs2.Stream[IO, (BigInt, ByteBuffer)] = {
                fs2.Stream eval IO {
                  val (key, kvs) = row
                  val bytes = ByteBuffer.wrap(AvroEncoder.toBinary(kvs)(codec))
                  (key, bytes)
                }
              }

              def retire(row: (BigInt, ByteBuffer)): fs2.Stream[IO, AsyncResultSet] = {
                val (id, value) = row
                fs2.Stream eval session.executeF[IO](writeStatement.bind(id.asJava, value))
              }

              val results = rows
                .flatMap(elaborateRow)
                .flatMap(rowToBytes)
                .map(retire)
                .parJoinUnbounded
                .onComplete { fs2.Stream eval IO(session.closeAsync) }

              results
                .compile
                .drain
                .attempt
                .unsafeRunSync()
                .valueOr(throw _)
            }
          }
        }
  }
}
