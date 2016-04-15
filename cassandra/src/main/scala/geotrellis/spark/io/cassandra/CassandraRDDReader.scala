package geotrellis.spark.io.cassandra

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import com.datastax.driver.core.{Row, Token}
import org.apache.avro.Schema
import org.apache.cassandra.hadoop.cql3.{CqlConfigHelper, CqlInputFormat}
import org.apache.cassandra.utils.ByteBufferUtil
import org.apache.hadoop.mapreduce.{InputFormat, Job}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object CassandraRDDReader {
  def read[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    table: String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None
  )(implicit sc: SparkContext, instance: CassandraInstance): RDD[(K, V)] = {
    if(queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val codec = KryoWrapper(KeyValueRecordCodec[K, V])
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)

    val job = Job.getInstance(sc.hadoopConfiguration)
    instance.setCassandraConfig(job)
    instance.setInputColumnFamily(job, table)

    val ranges = queryKeyBounds.flatMap(decomposeBounds)

    val kwWriterSchema = KryoWrapper(writerSchema)
    ranges.map { case (from, to) =>
      CqlConfigHelper.setInputCql(job.getConfiguration, s"SELECT * FROM ${instance.keySpace}.${table} WHERE key >= ${from} and key <= ${to} ALLOW FILTERING")

      sc.newAPIHadoopRDD(
        job.getConfiguration,
        classOf[CqlInputFormat].asInstanceOf[Class[InputFormat[Long, Row]]],
        classOf[Long],
        classOf[Row])
        .map { case (_, value) =>
          AvroEncoder
            .fromBinary(
              kwWriterSchema.value.getOrElse(codec.value.schema),
              ByteBufferUtil.getArray(value.getBytes("name")))(codec.value)
        }
        .flatMap { pairs: Vector[(K, V)] =>
          if(filterIndexOnly) pairs
          else pairs.filter { pair => includeKey(pair._1) }
        }
    }.reduce(_ ++ _)
  }
}
