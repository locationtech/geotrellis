package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.KeyCodecs._
import geotrellis.spark.io.index._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

object SpaceTimeRasterRDDReader extends RasterRDDReader[SpaceTimeKey] {
  def getCube(
    job: Job,
    layerId: LayerId,
    keyBounds: KeyBounds[SpaceTimeKey],
    keyIndex: KeyIndex[SpaceTimeKey]
  )(implicit sc: SparkContext): RDD[(Key, Value)] = {
    val ranges = 
      keyIndex.indexRanges(keyBounds) 
      .map { case (min: Long, max: Long) =>
        new ARange(rowId(layerId, min), true, rowId(layerId, max), true)
      }

    InputFormatBase.setRanges(job, ranges)

    val minTime = keyBounds.minKey.temporalKey.time
    val maxTime = keyBounds.maxKey.temporalKey.time

    val props =  Map(
      "startBound" -> minTime.toString,
      "endBound" -> maxTime.toString,
      "startInclusive" -> "true",
      "endInclusive" -> "true")

    InputFormatBase.addIterator(job,
      new IteratorSetting(2, "TimeColumnFilter", "org.apache.accumulo.core.iterators.user.ColumnSliceFilter", props))

    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)

    sc.newAPIHadoopRDD(
      job.getConfiguration,
      classOf[BatchAccumuloInputFormat],
      classOf[Key],
      classOf[Value]
    )
  }
}
