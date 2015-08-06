package geotrellis.spark.io.avro

import org.apache.avro.Schema

trait AvroCodec[T, R] {
  def schema: Schema
  def encode(thing: T): R
  def decode(rep: R): T
}
