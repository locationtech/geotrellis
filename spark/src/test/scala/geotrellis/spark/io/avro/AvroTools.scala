package geotrellis.spark.io.avro

import org.scalatest._

trait AvroTools { self: Matchers =>
  def roundTrip[T](thing: T)(implicit codec: AvroRecordCodec[T]): Unit = {
    val bytes = AvroEncoder.toBinary(thing)
    val fromBytes = AvroEncoder.fromBinary[T](bytes)
    fromBytes should be equals thing
    val json = AvroEncoder.toJson(thing)
    val fromJson = AvroEncoder.fromJson[T](json)
    fromJson should be equals thing
  }
}
