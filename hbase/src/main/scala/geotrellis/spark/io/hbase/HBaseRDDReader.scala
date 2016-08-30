package geotrellis.spark.io.hbase

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds, LayerId}

import org.apache.avro.Schema
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.filter.PrefixFilter
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.{IdentityTableMapper, MultiTableInputFormat, TableMapReduceUtil}
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object HBaseRDDReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: HBaseInstance,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if (queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) // Avro Schema is not Serializable

    val ranges: Seq[(Long, Long)] = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val filter = new PrefixFilter(HBaseRDDWriter.layerIdString(layerId))
    val scans = java.util.Arrays.asList(ranges.map { case (start, stop) =>
      HBaseUtils.buildScan(
        table, HBaseRDDWriter.tilesCF,
        HBaseKeyEncoder.encode(layerId, start),
        HBaseKeyEncoder.encode(layerId, stop, trailingByte = true), // add trailing byte, to include stop row
        filter)
    }: _*)

    val conf = sc.hadoopConfiguration
    HBaseConfiguration.merge(conf, instance.conf)

    val job = Job.getInstance(conf)
    TableMapReduceUtil.initCredentials(job)
    TableMapReduceUtil.initTableMapperJob(scans, classOf[IdentityTableMapper], null, null, job)

    val jconf = new JobConf(job.getConfiguration)
    SparkHadoopUtil.get.addCredentials(jconf)

    sc.newAPIHadoopRDD(
      jconf,
      classOf[MultiTableInputFormat],
      classOf[ImmutableBytesWritable],
      classOf[Result]
    ).flatMap { case (_, row) =>
      val bytes = row.getValue(HBaseRDDWriter.tilesCF, "")
      val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
      if (filterIndexOnly) recs
      else recs.filter { row => includeKey(row._1) }
    }
  }
}
