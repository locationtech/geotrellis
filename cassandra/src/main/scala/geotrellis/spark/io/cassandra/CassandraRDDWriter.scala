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

import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.LayerId
import geotrellis.spark.util.KryoWrapper

import com.datastax.driver.core.DataType._
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.schemabuilder.SchemaBuilder

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD

import com.typesafe.config.ConfigFactory

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import java.nio.ByteBuffer
import java.util.concurrent.Executors

import scala.collection.JavaConversions._


object CassandraRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    rdd: RDD[(K, V)],
    instance: CassandraInstance,
    layerId: LayerId,
    decomposeKey: K => Long,
    keyspace: String,
    table: String,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.cassandra.threads.rdd.write")
  ): Unit = update(rdd, instance, layerId, decomposeKey, keyspace, table, None, None, threads)

  private[cassandra] def update[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: CassandraInstance,
    layerId: LayerId,
    decomposeKey: K => Long,
    keyspace: String,
    table: String,
    writerSchema: Option[Schema],
    mergeFunc: Option[(V,V) => V],
    threads: Int = ConfigFactory.load().getThreads("geotrellis.cassandra.threads.rdd.write")
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec = KeyValueRecordCodec[K, V]

    instance.withSessionDo { session =>
      instance.ensureKeyspaceExists(keyspace, session)
      session.execute(
        SchemaBuilder.createTable(keyspace, table).ifNotExists()
          .addPartitionKey("key", bigint)
          .addClusteringColumn("name", text)
          .addClusteringColumn("zoom", cint)
          .addColumn("value", blob)
      )
    }

    val readQuery =
      QueryBuilder.select("value")
        .from(keyspace, table)
        .where(eqs("key", QueryBuilder.bindMarker()))
        .and(eqs("name", layerId.name))
        .and(eqs("zoom", layerId.zoom))
        .toString

    val writeQuery =
      QueryBuilder
        .insertInto(keyspace, table)
        .value("name", layerId.name)
        .value("zoom", layerId.zoom)
        .value("key", QueryBuilder.bindMarker())
        .value("value", QueryBuilder.bindMarker())
        .toString

    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema)

    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.
      raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .foreachPartition { partition =>
          if(partition.nonEmpty) {
            instance.withSession { session =>
              val readStatement = session.prepare(readQuery)
              val writeStatement = session.prepare(writeQuery)

              val queries: Process[Task, (java.lang.Long, ByteBuffer)] =
                Process.unfold(partition) { iter =>
                  if (iter.hasNext) {
                    val recs = iter.next()
                    val key: java.lang.Long = recs._1
                    val rows1: Vector[(K,V)] = recs._2.toVector
                    val rows2: Vector[(K,V)] =
                      if (mergeFunc != None) {
                        val row = session.execute(readStatement.bind(key))
                        if (row.nonEmpty) {
                          val bytes = row.one().getBytes("value").array()
                          val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                          recs
                        } else Vector.empty
                      }
                      else Vector.empty

                    val outRows: Vector[(K, V)] =
                      mergeFunc match {
                        case Some(fn) =>
                          (rows2 ++ rows1)
                            .groupBy({ case (k,v) => k })
                            .map({ case (k, kvs) =>
                              val vs = kvs.map({ case (k,v) => v }).toSeq
                              val v: V = vs.tail.foldLeft(vs.head)(fn)
                              (k, v) })
                            .toVector
                        case None => rows1
                      }
                    val bytes = ByteBuffer.wrap(AvroEncoder.toBinary(outRows)(codec))
                    Some((key, bytes), iter)
                  } else {
                    None
                  }
                }

              val pool = Executors.newFixedThreadPool(threads)

              val write: ((java.lang.Long, ByteBuffer)) => Process[Task, ResultSet] = {
                case (id, value) =>
                  Process eval Task {
                    session.execute(writeStatement.bind(id, value))
                  }(pool)
              }

              val results = nondeterminism.njoin(maxOpen = threads, maxQueued = threads) {
                queries map write
              }(Strategy.Executor(pool)) onComplete {
                Process eval Task {
                  session.closeAsync()
                  session.getCluster.closeAsync()
                }(pool)
              }

              results.run.unsafePerformSync
              pool.shutdown()
            }
          }
        }
  }
}
