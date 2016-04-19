package geotrellis.spark.io.cassandra

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder
import org.apache.spark.rdd.RDD
import com.datastax.driver.core.DataType._
import org.apache.cassandra.hadoop.cql3.{CqlBulkOutputFormat, _}
import org.apache.cassandra.utils.ByteBufferUtil
import org.apache.hadoop.mapreduce.Job
import geotrellis.spark.LayerId
import org.apache.cassandra.hadoop.ConfigHelper

import scala.collection.JavaConversions._
import java.nio.ByteBuffer
import java.util

import org.apache.cassandra.config.Config
import org.apache.cassandra.io.sstable.CQLSSTableWriter

object CassandraRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: CassandraInstance,
    decomposeKey: K => (Long, LayerId),
    table: String
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec   = KeyValueRecordCodec[K, V]
    val schema  = codec.schema
    val session = instance.session

    Config.setClientMode(true)
    val job = Job.getInstance(sc.hadoopConfiguration)
    ConfigHelper.setOutputColumnFamily(job.getConfiguration, instance.keySpace, table)
    ConfigHelper.setOutputRpcPort(job.getConfiguration, instance.port)
    ConfigHelper.setOutputInitialAddress(job.getConfiguration, instance.host)
    ConfigHelper.setOutputPartitioner(job.getConfiguration, "ByteOrderedPartitioner")
    job.setOutputFormatClass(classOf[CqlBulkOutputFormat])
    CqlBulkOutputFormat.setTableSchema(
      job.getConfiguration,
      table,
      s"""CREATE TABLE IF NOT EXISTS ${instance.keySpace}.${table} (
         |key bigint,
         |name text,
         |zoom int,
         |value blob,
         |PRIMARY KEY (key, (name, zoom))
         |)""".stripMargin
    )
    CqlBulkOutputFormat.setTableInsertStatement(
      job.getConfiguration,
      table,
      s"INSERT INTO ${instance.keySpace}.${table} (key, name, zoom, value) VALUES (?, ?, ?, ?)"
    )

    val kvPairs: RDD[(util.Map[String, ByteBuffer], util.List[ByteBuffer])] =
      raster
        .groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .map { case ((key, layerId), pairs) =>
          val outKey = Map(
            "key"  -> ByteBufferUtil.bytes(key.toString),
            "name" -> ByteBufferUtil.bytes(layerId.name),
            "zoom" -> ByteBufferUtil.bytes(layerId.zoom.toString)
          )

          val outVal = ByteBuffer.wrap(AvroEncoder.toBinary(pairs.toVector)(codec)) :: Nil
          (outKey, outVal)
        }
        //.sortByKey()

    kvPairs.saveAsNewAPIHadoopFile(
      instance.keySpace,
      classOf[util.Map[String, ByteBuffer]],
      classOf[util.List[ByteBuffer]],
      classOf[CqlBulkOutputFormat],
      job.getConfiguration
    )
  }
}
