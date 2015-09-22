package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.utils.KryoWrapper
import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Range => ARange, Value}
import org.apache.accumulo.core.util.{Pair => APair}
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

class SpaceTimeAccumuloRDDReader[V: AvroRecordCodec: ClassTag](instance: AccumuloInstance)
  extends IAccumuloRDDReader[SpaceTimeKey, V] {

  type K = SpaceTimeKey

  def read(
      table: String,
      columnFamily: Text,
      getRow: Long => Text,
      keyIndex: KeyIndex[K],
      queryKeyBounds: Seq[KeyBounds[K]],
      writerSchema: Schema,
      iterators: Seq[IteratorSetting] = Seq.empty)
    (implicit sc: SparkContext): RDD[(K, V)] = {

    val codec = KryoWrapper(KeyValueRecordCodec[K, V])
    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)

    queryKeyBounds
      .map { bound =>
        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, table)

        val ranges =
          keyIndex.indexRanges(bound)
          .map { case (min, max) => new ARange(getRow(min), true, getRow(max), true) }
          .asJava

        InputFormatBase.setRanges(job, ranges)
        InputFormatBase.fetchColumns(job, List(new APair(columnFamily, null: Text)).asJava)
        InputFormatBase.addIterator(job,
          new IteratorSetting(2,
            "TimeColumnFilter",
            "org.apache.accumulo.core.iterators.user.ColumnSliceFilter",
            Map("startBound" -> bound.minKey.time.toString,
              "endBound" -> bound.maxKey.time.toString,
              "startInclusive" -> "true",
              "endInclusive" -> "true").asJava))

        iterators.foreach(InputFormatBase.addIterator(job, _))

        sc.newAPIHadoopRDD(
          job.getConfiguration,
          classOf[BatchAccumuloInputFormat],
          classOf[Key],
          classOf[Value])
        .map { case (_, value) =>
          AvroEncoder.fromBinary(writerSchema, value.get)(codec.value)
        }
        .flatMap { pairs: Vector[(K, V)] =>
          pairs.filter(pair => includeKey(pair._1))
        }
      }
      .reduce(_ union _)
  }


}
