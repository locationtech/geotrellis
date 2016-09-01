package geotrellis.spark.io.avro.codecs

import geotrellis.spark.io.avro._
import geotrellis.spark.{SpaceTimeKey, SpatialKey}
import org.apache.avro._
import org.apache.avro.generic._
import org.joda.time.{DateTime, DateTimeZone}

// --- //

/** Serialize any [[Array[Byte]]] via Avro. */
trait ByteArrayCodec {
  implicit def byteArrayAvroFormat = new AvroRecordCodec[Array[Byte]] {
    def schema: Schema = SchemaBuilder
      .record("ByteArray").namespace("geotrellis.spark")
      .fields()
      .name("bytes").`type`().bytesType().noDefault()
      .endRecord()

    def encode(bytes: Array[Byte], rec: GenericRecord): Unit = {
      rec.put("bytes", bytes)
    }

    def decode(rec: GenericRecord): Array[Byte] = {
      rec[Array[Byte]]("bytes")
    }
  }
}
