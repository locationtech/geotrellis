package geotrellis.spark.io.avro

import org.apache.avro.generic.GenericRecord
import org.apache.avro._
import scala.collection.JavaConverters._

class KeyValueRecordCodec[K, V](implicit a: AvroRecordCodec[K], b: AvroRecordCodec[V]) extends AvroRecordCodec[Vector[(K, V)]] {
  val pairCodec = new TupleCodec[K,V]

  def schema = SchemaBuilder
    .record("KeyValueRecord").namespace("geotrellis.spark.io")
    .fields()
    .name("pairs").`type`().array().items.`type`(pairCodec.schema).noDefault()
    .endRecord()

  def encode(t: Vector[(K, V)], rec: GenericRecord) = {
    rec.put("pairs", t.map(pairCodec.encode).asJavaCollection)
  }

  def decode(rec: GenericRecord) = {
    rec.get("pairs")
      .asInstanceOf[java.util.Collection[GenericRecord]]
      .asScala
      .map(pairCodec.decode)
      .toVector
  }
}

object KeyValueRecordCodec {
  def apply[K: AvroRecordCodec, V: AvroRecordCodec] = new KeyValueRecordCodec[K, V]
}
