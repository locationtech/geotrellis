package geotrellis.spark.io.accumulo

import geotrellis.spark._

import org.apache.hadoop.io.Text

package object encoder {
  def long2Bytes(x: Long): Array[Byte] =
    Array[Byte](x>>56 toByte, x>>48 toByte, x>>40 toByte, x>>32 toByte, x>>24 toByte, x>>16 toByte, x>>8 toByte, x toByte)

  def index2RowId(index: Long): Text = new Text(long2Bytes(index))
}
