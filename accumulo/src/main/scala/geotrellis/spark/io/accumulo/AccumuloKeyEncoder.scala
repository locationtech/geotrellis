package geotrellis.spark.io.accumulo

import geotrellis.spark._

import org.apache.accumulo.core.data.Key
import org.apache.hadoop.io.Text

object AccumuloKeyEncoder {
  final def long2Bytes(x: Long): Array[Byte] =
    Array[Byte](x>>56 toByte, x>>48 toByte, x>>40 toByte, x>>32 toByte, x>>24 toByte, x>>16 toByte, x>>8 toByte, x toByte)

  final def index2RowId(index: Long): Text = new Text(long2Bytes(index))

  def encode[K](id: LayerId, key: K, index: Long): Key =
    new Key(index2RowId(index), columnFamily(id))

  def getLocalityGroups(id: LayerId): Seq[String] = Seq(columnFamily(id))
}
