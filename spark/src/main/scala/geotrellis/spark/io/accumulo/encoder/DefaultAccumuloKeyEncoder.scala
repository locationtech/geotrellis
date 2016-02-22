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

class DefaultAccumuloKeyEncoder[K] extends AccumuloKeyEncoder[K] {
  def encode(id: LayerId, key: K, index: Long): Key =
    new Key(index2RowId(index), columnFamily(id))

  def setupQuery(id: LayerId, queryKeyBounds: Seq[KeyBounds[K]], keyIndex: KeyIndex[K])(getJob: () => Job): Seq[Job] = {
    val cf = new Text(columnFamily(id))

    val ranges =
      queryKeyBounds
        .flatMap { bounds =>
          keyIndex.indexRanges(bounds).map { case (min, max) =>
            new AccumuloRange(new Text(long2Bytes(min)), new Text(long2Bytes(max)))
          }
        }
        .asJava

    val job = getJob()
    InputFormatBase.setRanges(job, ranges)
    InputFormatBase.fetchColumns(job, List(new AccumuloPair(cf, null: Text)).asJava)
    Seq(job)
  }

  def getLocalityGroups(id: LayerId): Seq[String] = Seq(columnFamily(id))
}
