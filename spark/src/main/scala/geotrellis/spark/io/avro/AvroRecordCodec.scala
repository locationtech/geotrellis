package geotrellis.spark.io.avro

import org.apache.avro.generic._
import org.apache.avro._
import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

@implicitNotFound("Cannot find AvroRecordCodec for ${T}. Try to import geotrellis.spark.io.avro.codecs._")
abstract class AvroRecordCodec[T: ClassTag] extends AvroCodec[T, GenericRecord] {
  def schema: Schema
  def encode(thing: T, rec: GenericRecord)
  def decode(rec: GenericRecord): T

  def encode(thing: T): GenericRecord = {
    val rec = new GenericData.Record(schema)
    encode(thing, rec)
    rec
  }

  def supported[O](other: O): Boolean = {
    implicitly[ClassTag[T]].unapply(other).isDefined
  }
}
