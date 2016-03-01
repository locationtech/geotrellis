package geotrellis.spark.io.accumulo

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.utils.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import org.apache.accumulo.core.client.mapreduce.{AccumuloInputFormat, InputFormatBase}
import org.apache.accumulo.core.data.{Range => AccumuloRange, Value, Key}
import org.apache.accumulo.core.util.{Pair => AccumuloPair}
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import scala.collection.JavaConverters._

object AccumuloRDDReader {
  def read[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    table: String,
    columnFamily: Text,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[AccumuloRange],
    writerSchema: Option[Schema] = None
  )(implicit sc: SparkContext, instance: AccumuloInstance): RDD[(K, V)] = {

    val codec = KryoWrapper(KeyValueRecordCodec[K, V])
    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)(boundable)

    val job = Job.getInstance(sc.hadoopConfiguration)
    instance.setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)

    val ranges = queryKeyBounds.flatMap(decomposeBounds).asJava
    InputFormatBase.setRanges(job, ranges)
    InputFormatBase.fetchColumns(job, List(new AccumuloPair(columnFamily, null: Text)).asJava)
    InputFormatBase.setBatchScan(job, true)

    val kwWriterSchema = KryoWrapper(writerSchema)
    sc.newAPIHadoopRDD(
      job.getConfiguration,
      classOf[AccumuloInputFormat],
      classOf[Key],
      classOf[Value])
    .map { case (_, value) =>
      AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.value.schema), value.get)(codec.value)
    }
    .flatMap { pairs: Vector[(K, V)] =>
      pairs.filter { pair => includeKey(pair._1) }
    }
  }
}
