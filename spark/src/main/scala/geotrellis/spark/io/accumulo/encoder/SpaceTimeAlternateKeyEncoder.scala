package geotrellis.spark.io.accumulo.encoder

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._

import org.apache.accumulo.core.client.IteratorSetting
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.Key
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.accumulo.core.util.{Pair => AccumuloPair}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.joda.time.DateTimeZone

import scala.collection.JavaConverters._
import scala.reflect._

class SpaceTimeAlternateAccumuloKeyEncoder extends AccumuloKeyEncoder[SpaceTimeKey] {
  def encode(id: LayerId, key: SpaceTimeKey, index: Long): Key =
    new Key(index2RowId(index), columnFamily(id), new Text(key.time.withZone(DateTimeZone.UTC).toString))

  def setupQuery(id: LayerId, queryKeyBounds: Seq[KeyBounds[SpaceTimeKey]], keyIndex: KeyIndex[SpaceTimeKey])(getJob: () => Job): Seq[Job] = {
    val cf = new Text(columnFamily(id))

    queryKeyBounds
      .map { bounds =>
        val job = getJob()
        val ranges =
          keyIndex.indexRanges(bounds).map { case (min, max) =>
            new AccumuloRange(new Text(long2Bytes(min)), new Text(long2Bytes(max)))
          }

        InputFormatBase.setRanges(job, ranges.asJava)
        InputFormatBase.fetchColumns(job, List(new AccumuloPair(cf, null: Text)).asJava)
        InputFormatBase.addIterator(job,
          new IteratorSetting(2,
            "TimeColumnFilter",
            "org.apache.accumulo.core.iterators.user.ColumnSliceFilter",
            Map("startBound" -> bounds.minKey.time.toString,
              "endBound" -> bounds.maxKey.time.toString,
              "startInclusive" -> "true",
              "endInclusive" -> "true").asJava))
        job
      }
  }

  def getLocalityGroups(id: LayerId): Seq[String] = Seq(columnFamily(id))
}
