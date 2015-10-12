package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.avro._

import org.apache.hadoop.io._

import java.io._

import scala.reflect._

/**
 * In order to implement WritableComparable correctly for our subtypes we have to use F-Bounded Polymorphism.
 * When extending this class the implicit parameters are going to be resolved and become class fields,
 * yielding a concrete class with zero argument constructor just as Java likes.
 */
abstract class AvroKeyWritable[T: AvroRecordCodec: Ordering, F <: AvroKeyWritable[T, F]]
  extends WritableComparable[F]
  with IndexedKeyWritable[T]
  with Serializable {

  private var _index: Long = 0L
  private var _value: T = null.asInstanceOf[T]

  def index = _index
  def key = _value

  def set(i: Long, k: T): Unit = {
    _index = i
    _value = k
  }

  def setIndex(i: Long): Unit = {
    _index = i
  }

  def get(): (Long, T) = (_index, _value)

  def write(out: DataOutput): Unit = {
    out.writeLong(_index)
    val bytes = AvroEncoder.toBinary(_value)
    out.writeInt(bytes.length)
    out.write(bytes, 0, bytes.length)
  }

  def readFields(in: DataInput): Unit = {
    _index = in.readLong
    val len = in.readInt
    val bytes = new Array[Byte](len)
    in.readFully(bytes)
    _value = AvroEncoder.fromBinary(bytes)
  }

  override def hashCode = get().hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case kw: AvroKeyWritable[T, F] =>
        kw.index == index && kw.key == key
      case _ => false
    }
  }

  def compareTo(other: F): Int = {
    if (this._index < other._index) -1
    else if (this._index > other._index) 1
    else {
      // nulls are possible if empty AvroWritable is created with only Index
      if (null == other.key) 1
      else if (null == _value) -1
      else if (implicitly[Ordering[T]].lt(this._value, other._value)) -1
      else if (this._value == other._value) 0
      else 1
    }
  }
}

object AvroKeyWritable {
  implicit def ordering[T, F <: AvroKeyWritable[T, F]]: Ordering[F] = new Ordering[F] {
    def compare(x: F, y: F) = x.compareTo(y)
  }


  def init[T, A <: AvroKeyWritable[T, A]: ClassTag](idx: Long, k: T): A = {
    val a = classTag[A].runtimeClass.newInstance().asInstanceOf[A]
    a.set(idx, k)
    a
  }
}