package geotrellis.spark.io.hbase

import geotrellis.spark.LayerId
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._

import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.spark.rdd.RDD

object HBaseRDDWriter {

  val tilesCF = "tiles"
  val SEP = "__.__"
  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}|"

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: HBaseInstance,
    layerId: LayerId,
    decomposeKey: K => Long,
    table: String
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec = KeyValueRecordCodec[K, V]

    //create the attribute table if it does not exist
    if (!instance.getAdmin.tableExists(table)) {
      val tableDesc = new HTableDescriptor(table: TableName)
      val idsColumnFamilyDesc = new HColumnDescriptor(tilesCF)
      tableDesc.addFamily(idsColumnFamilyDesc)
      instance.getAdmin.createTable(tableDesc)
    }

    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.

    raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
      .foreachPartition { partition: Iterator[(Long, Iterable[(K, V)])] =>
        val mutator = instance.getConnection.getBufferedMutator(table)

        partition.foreach { recs =>
          val id = recs._1
          val pairs = recs._2.toVector
          val bytes = AvroEncoder.toBinary(pairs)(codec)
          val put = new Put(HBaseKeyEncoder.encode(layerId, id))
          put.addColumn(tilesCF, "", System.currentTimeMillis(), bytes)
          mutator.mutate(put)
        }

        mutator.flush()
        mutator.close()
      }
  }
}
