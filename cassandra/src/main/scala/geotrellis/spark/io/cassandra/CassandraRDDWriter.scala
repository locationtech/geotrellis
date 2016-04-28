package geotrellis.spark.io.cassandra

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.LayerId
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.DataType._
import com.datastax.driver.core.{ResultSet, ResultSetFuture}
import org.apache.spark.rdd.RDD

import scalaz.concurrent.Task
import scalaz.stream.{Process, nondeterminism}
import java.nio.ByteBuffer
import java.util.concurrent.Executors

import scala.collection.JavaConversions._

object CassandraRDDWriter {
  type KV = ((java.lang.Long, java.lang.String, java.lang.Integer), ByteBuffer)

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: CassandraInstance,
    decomposeKey: K => (Long, LayerId),
    table: String
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    instance.withSession {
      _.execute(
        SchemaBuilder.createTable(instance.keyspace, table).ifNotExists()
          .addPartitionKey("key", bigint)
          .addClusteringColumn("name", text)
          .addClusteringColumn("zoom", cint)
          .addColumn("value", blob)
      )
    }

    val query =
      QueryBuilder
        .insertInto(instance.keyspace, table)
        .value("key", QueryBuilder.bindMarker())
        .value("name", QueryBuilder.bindMarker())
        .value("zoom", QueryBuilder.bindMarker())
        .value("value", QueryBuilder.bindMarker())
        .toString

    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.
      raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .foreachPartition { partition =>
          import geotrellis.spark.util.TaskUtils._
          instance.withSession { session =>
            val statement = session.prepare(query)

            /*val queries: Iterator[Option[ResultSetFuture]] = (partition.map { recs =>
              val ((key, layerId), pairs) = (recs._1, recs._2.toVector)
              val bytes = ByteBuffer.wrap(AvroEncoder.toBinary(pairs)(codec))
              Some(session.executeAsync(
                statement.bind(
                  key.asInstanceOf[java.lang.Integer],
                  layerId.name,
                  layerId.zoom.asInstanceOf[java.lang.Integer],
                  bytes
                )
              ))
            }) ++ Iterator({
              session.closeAsync(); session.getCluster.closeAsync(); Option.empty[ResultSetFuture]
            })

            queries.map(_.map((_: ResultSetFuture).getUninterruptibly()))*/

            val queries: Process[Task, KV] =
              Process.unfold(partition) { iter =>
                if (iter.hasNext) {
                  val recs = iter.next()
                  val (id, layerId) = recs._1
                  val pairs = recs._2.toVector
                  val bytes = ByteBuffer.wrap(AvroEncoder.toBinary(pairs)(codec))
                  Some(((id, layerId.name, layerId.zoom), bytes), iter)
                } else {
                  None
                }
              }

            /** magic number 8; for no reason; just because */
            val pool = Executors.newFixedThreadPool(8)

            val write: KV => Process[Task, ResultSet] = {
              case ((id, name, zoom), value) =>
                Process eval Task {
                  session.execute(statement.bind(id, name, zoom, value))
                }(pool).retryEBO {
                  case _ => false
                }
            }

            val results = nondeterminism.njoin(maxOpen = 8, maxQueued = 8) {
              queries map write
            } onComplete {
              Process eval Task {
                session.closeAsync()
                session.getCluster.closeAsync()
              }
            }

            results.run.unsafePerformSync
            pool.shutdown()
          }
        }
  }
}
