package geotrellis.spark.io.cassandra

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.LayerId

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.DataType._
import com.datastax.driver.core.ResultSet
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import java.nio.ByteBuffer
import java.util.concurrent.Executors

import scala.collection.JavaConversions._

object CassandraRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: CassandraInstance,
    layerId: LayerId,
    decomposeKey: K => Long,
    keyspace: String,
    table: String,
    threads: Int = ConfigFactory.load().getInt("geotrellis.cassandra.threads.rdd.write")
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    instance.withSessionDo {
      _.execute(
        SchemaBuilder.createTable(keyspace, table).ifNotExists()
          .addPartitionKey("key", bigint)
          .addClusteringColumn("name", text)
          .addClusteringColumn("zoom", cint)
          .addColumn("value", blob)
      )
    }

    val query =
      QueryBuilder
        .insertInto(keyspace, table)
        .value("name", layerId.name)
        .value("zoom", layerId.zoom)
        .value("key", QueryBuilder.bindMarker())
        .value("value", QueryBuilder.bindMarker())
        .toString

    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.
      raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .foreachPartition { partition =>
          instance.withSession { session =>
            val statement = session.prepare(query)

            val queries: Process[Task, (java.lang.Long, ByteBuffer)] =
              Process.unfold(partition) { iter =>
                if (iter.hasNext) {
                  val recs = iter.next()
                  val id    = recs._1
                  val pairs = recs._2.toVector
                  val bytes = ByteBuffer.wrap(AvroEncoder.toBinary(pairs)(codec))
                  Some((id, bytes), iter)
                } else {
                  None
                }
              }

            val pool = Executors.newFixedThreadPool(threads)

            val write: ((java.lang.Long, ByteBuffer)) => Process[Task, ResultSet] = {
              case (id, value) =>
                Process eval Task {
                  session.execute(statement.bind(id, value))
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
