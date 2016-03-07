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

object AccumuloKeyEncoder {
  final def long2Bytes(x: Long): Array[Byte] =
    Array[Byte](x>>56 toByte, x>>48 toByte, x>>40 toByte, x>>32 toByte, x>>24 toByte, x>>16 toByte, x>>8 toByte, x toByte)

  final def index2RowId(index: Long): Text = new Text(long2Bytes(index))

  def encode[K](id: LayerId, key: K, index: Long): Key =
    new Key(index2RowId(index), columnFamily(id))

  def getLocalityGroups(id: LayerId): Seq[String] = Seq(columnFamily(id))
}
