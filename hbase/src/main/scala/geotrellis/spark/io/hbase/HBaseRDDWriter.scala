package geotrellis.spark.io.hbase

import geotrellis.spark.LayerId
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._

import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.{HTableDescriptor, TableName}
import org.apache.spark.rdd.RDD
import scalaz.concurrent.Task
import scalaz.stream.{Process, nondeterminism}
import java.util.concurrent.Executors

object HBaseRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: HBaseInstance,
    layerId: LayerId,
    decomposeKey: K => Long,
    table: String
  ): Unit = {
    implicit val sc = raster.sparkContext

    val admin = instance.getAdmin
    val codec = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    //create the attribute table if it does not exist
    if (!admin.tableExists(table)) admin.createTable(new HTableDescriptor(table: TableName))
    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.

    raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .foreachPartition { partition =>
          val mutator = instance.getAdmin.getConnection.getBufferedMutator(table)
          val queries: Process[Task, Put] =
            Process.unfold(partition) { iter =>
              if (iter.hasNext) {
                val recs = iter.next()
                val id = recs._1
                val pairs = recs._2.toVector
                val bytes = AvroEncoder.toBinary(pairs)(codec)
                val put = new Put(longToBytes(id))
                put.addColumn(columnFamily(layerId), "", System.currentTimeMillis(), bytes)
                Some(put, iter)
              } else {
                None
              }
            }

          /** magic number 32; for no reason; just because */
          val pool = Executors.newFixedThreadPool(32)

          val write: Put => Process[Task, Unit] = {
            case put => Process eval Task { mutator.mutate(put) }(pool)
          }

          val results = nondeterminism.njoin(maxOpen = 32, maxQueued = 32) {
            queries map write
          } onComplete {
            Process eval Task {
              mutator.flush()
              mutator.close()
            }
          }

          results.run.unsafePerformSync
          pool.shutdown()
        }
  }
}
