package geotrellis.spark.io.accumulo

import geotrellis.spark._
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

trait AccumuloKeyEncoderStrategy {
  def encoderFor[K: ClassTag](): AccumuloKeyEncoder[K]
}

object AccumuloKeyEncoderStrategy {
  def DEFAULT = DefaultKeyEncoderStrategy
}

object DefaultKeyEncoderStrategy extends AccumuloKeyEncoderStrategy {
  def encoderFor[K: ClassTag]() = new DefaultAccumuloKeyEncoder()
}

class CustomAccumuloKeyEncoderStrategy private (encoderMap: Map[Class[_], AccumuloKeyEncoder[_]]) {
  def withEncoder[K: ClassTag](keyEncoder: AccumuloKeyEncoder[K]): CustomAccumuloKeyEncoderStrategy =
    new CustomAccumuloKeyEncoderStrategy(encoderMap ++ Map(classTag[K].runtimeClass -> keyEncoder))

  def encoderFor[K: ClassTag](): AccumuloKeyEncoder[K] =
    encoderMap.get(classTag[K].runtimeClass) match {
      case Some(ke) => ke.asInstanceOf[AccumuloKeyEncoder[K]]
      case None => new DefaultAccumuloKeyEncoder()
    }
}

trait AccumuloKeyEncoder[K] {
  def encode(id: LayerId, key: K, index: Long): Key

  def setupQuery(id: LayerId, queryKeyBounds: Seq[KeyBounds[K]], keyIndex: KeyIndex[K])(getJob: () => Job): Seq[Job]

  def getLocalityGroups(id: LayerId): Seq[String]
}

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
