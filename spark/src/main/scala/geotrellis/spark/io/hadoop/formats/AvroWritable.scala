package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.avro._

import org.apache.hadoop.io.Writable
import java.io.{ DataInput, DataOutput }

class AvroWritable[T: AvroRecordCodec] extends Writable with Serializable {
  private var _bytes: Array[Byte] = Array()

  def write(out: DataOutput): Unit = {
    val size = _bytes.length
    out.writeInt(size)
    out.write(_bytes, 0, size)
  }

  def readFields(in: DataInput): Unit = {
    val size = in.readInt
    _bytes = Array.ofDim[Byte](size)
    in.readFully(_bytes, 0, size)
  }

  def set(thing: T): Unit =
    if (null != thing)
      _bytes = AvroEncoder.toBinary(thing)
    else
      _bytes = Array()

  def get(): T = {
    AvroEncoder.fromBinary(_bytes)
  }
}