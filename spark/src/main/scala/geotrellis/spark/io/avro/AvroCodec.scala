package geotrellis.spark.io.avro

import org.apache.avro.Schema

trait AvroCodec[T, R] extends Serializable {
  def schema: Schema
  def encode(thing: T): R
  def decode(rep: R): T
}
