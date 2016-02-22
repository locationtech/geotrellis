package geotrellis.spark.io.accumulo.encoder

import geotrellis.spark._
import geotrellis.spark.io.index._

import org.apache.accumulo.core.client.BatchDeleter
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

trait AccumuloKeyEncoder[K] {
  def encode(id: LayerId, key: K, index: Long): Key

  def setupQuery(id: LayerId, queryKeyBounds: Seq[KeyBounds[K]], keyIndex: KeyIndex[K])(getJob: () => Job): Seq[Job]

  def getLocalityGroups(id: LayerId): Seq[String]
}
