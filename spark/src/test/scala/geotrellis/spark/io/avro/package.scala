package geotrellis.spark.io

import org.scalatest._

package object avro extends Assertions {
  def roundTrip[T](thing: T)(implicit codec: AvroRecordCodec[T]): Unit = {
    val bytes = AvroEncoder.toBinary(thing)
    val fromBytes = AvroEncoder.fromBinary[T](bytes)
    assert(fromBytes === thing)
    val json = AvroEncoder.toJson(thing)
    val fromJson = AvroEncoder.fromJson[T](json)
    assert(fromJson === thing)
  }
}
