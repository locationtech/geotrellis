package geotrellis.spark.io.accumulo

import geotrellis.spark.io.avro.codecs.TupleCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.utils.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Range => ARange, Value, Key}
import org.apache.accumulo.core.util.{Pair => APair}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import scala.collection.JavaConverters._

abstract class AccumuloRDDReader[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag] {
  def read(
      instance: AccumuloInstance,
      table: String,
      getRow: Long => String,
      columnFamily: String,
      keyBounds: KeyBounds[K],
      keyIndex: KeyIndex[K],
      queryKeyBounds: Seq[KeyBounds[K]],
      iterators: Seq[IteratorSetting] = Seq.empty)
    (implicit sc: SparkContext): RDD[(K, V)] = {

    val codec = KryoWrapper(TupleCodec[K, V])
    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)

    val job = Job.getInstance(sc.hadoopConfiguration)
    instance.setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)

    val ranges = queryKeyBounds
      .flatMap { keyIndex.indexRanges(_) }
      .map { case (min, max) => new ARange(getRow(min), true, getRow(max), true) }
      .asJava

    InputFormatBase.setRanges(job, ranges)
    InputFormatBase.fetchColumns(job, List(new APair(new Text(columnFamily), null: Text)).asJava)
    iterators.foreach(InputFormatBase.addIterator(job, _))

    sc.newAPIHadoopRDD(
      job.getConfiguration,
      classOf[BatchAccumuloInputFormat],
      classOf[Key],
      classOf[Value]
    ).map { case (_, value) =>
      AvroEncoder.fromBinary(value.get)(codec.value)
    }.filter { row => includeKey(row._1) }
  }
}
