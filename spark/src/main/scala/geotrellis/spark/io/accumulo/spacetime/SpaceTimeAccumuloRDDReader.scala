package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Range => AccumuloRange, Key, Value}
import org.apache.accumulo.core.util.{Pair => APair}
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

class SpaceTimeAccumuloRDDReader[V: AvroRecordCodec: ClassTag](instance: AccumuloInstance)
  extends BaseAccumuloRDDReader[SpaceTimeKey, V] {

  def read(
      table: String,
      columnFamily: Text,
      queryKeyBounds: Seq[KeyBounds[SpaceTimeKey]],
      decomposeBounds: KeyBounds[SpaceTimeKey] => Seq[AccumuloRange],
      writerSchema: Option[Schema])
    (implicit sc: SparkContext): RDD[(SpaceTimeKey, V)] = {

    val codec = KryoWrapper(KeyValueRecordCodec[SpaceTimeKey, V])
    val includeKey = (key: SpaceTimeKey) => KeyBounds.includeKey(queryKeyBounds, key)(SpaceTimeKey.Boundable)
    val kwWriterSchema = KryoWrapper(writerSchema)

    queryKeyBounds
      .map { bound =>
        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, table)

        val ranges = decomposeBounds(bound).asJava
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

        sc.newAPIHadoopRDD(
          job.getConfiguration,
          classOf[BatchAccumuloInputFormat],
          classOf[Key],
          classOf[Value])
      }
      .map { rdd =>
        rdd.map { case (_, value) =>
          AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.value.schema), value.get)(codec.value)
        }
        .flatMap { pairs =>
          pairs.filter{ pair => includeKey(pair._1) }
        }
      }
      .reduce(_ union _)
  }
}
