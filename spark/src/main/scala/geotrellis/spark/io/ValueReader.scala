package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._

import spray.json._

import scala.reflect._

/** A key-value reader producer to read a layer one value at a time.
 * This interface abstracts over various construction requirements for
 * constructing a storage back-end specific reader. */
trait ValueReader[ID] {

  /** Produce a key value reader for a specific layer, prefetching layer metadata once at construction time */
  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: ID): Reader[K, V]
}
