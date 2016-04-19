package geotrellis.spark.io.cassandra

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import com.datastax.driver.core.{Row, Token}
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import org.apache.avro.Schema
import org.apache.cassandra.db.marshal.CompositeType
import org.apache.cassandra.hadoop.ConfigHelper
import org.apache.cassandra.hadoop.cql3.{BatchConfigHelper, CqlBatchInputFormat, CqlConfigHelper, CqlInputFormat}
import org.apache.cassandra.thrift.KeyRange
import org.apache.cassandra.utils.ByteBufferUtil
import org.apache.hadoop.mapreduce.{InputFormat, Job}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object CassandraRDDReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    table: String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[KeyRange],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None
  )(implicit sc: SparkContext, instance: CassandraInstance): RDD[(K, V)] = {
    if (queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val codec = KryoWrapper(KeyValueRecordCodec[K, V])
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)

    val job = Job.getInstance(sc.hadoopConfiguration)
    instance.setCassandraConfig(job)
    instance.setInputColumnFamily(job, table)
    ConfigHelper.setInputPartitioner(job.getConfiguration, "ByteOrderedPartitioner")

    val ranges = queryKeyBounds.flatMap(decomposeBounds)
    val kwWriterSchema = KryoWrapper(writerSchema)

    BatchConfigHelper.setInputKeyRanges(job.getConfiguration, ranges)
    sc.newAPIHadoopRDD(
      job.getConfiguration,
      classOf[CqlBatchInputFormat].asInstanceOf[Class[InputFormat[Long, Row]]],
      classOf[Long],
      classOf[Row])
      .map { case (_, value) =>
        AvroEncoder
          .fromBinary(
            kwWriterSchema.value.getOrElse(codec.value.schema),
            ByteBufferUtil.getArray(value.getBytes("name")))(codec.value)
      }
      .flatMap { pairs: Vector[(K, V)] =>
        if (filterIndexOnly) pairs
        else pairs.filter { pair => includeKey(pair._1) }
      }
  }
}
