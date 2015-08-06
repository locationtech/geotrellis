package geotrellis.spark.io.avro

import geotrellis.spark.{SpaceTimeKey, SpatialKey}
import org.apache.avro.generic._
import org.apache.avro._
import org.joda.time.{DateTime, DateTimeZone}

object KeyCodecs {
  implicit object SpatialKeyAvroFormat extends AvroRecordCodec[SpatialKey] {
    def schema = SchemaBuilder
      .record("SpatialKey").namespace("geotrellis.spark")
      .fields()
      .name("col").`type`().intType().noDefault()
      .name("row").`type`().intType().noDefault()
      .endRecord()

    def encode(thing: SpatialKey, rec: GenericRecord) = {
      rec.put("row", thing.row)
      rec.put("col", thing.col)
    }

    def decode(rec: GenericRecord): SpatialKey =
      SpatialKey(
        rec.get("col").asInstanceOf[Int],
        rec.get("row").asInstanceOf[Int])

  }

  implicit object SpaceTimeKeyAvroFormat extends AvroRecordCodec[SpaceTimeKey] {
    def schema = SchemaBuilder
      .record("SpaceTimeKey").namespace("geotrellis.spark")
      .fields()
      .name("col").`type`().intType().noDefault()
      .name("row").`type`().intType().noDefault()
      .name("millis").`type`().longType().noDefault()
      .name("offset").`type`().intType().noDefault()
      .endRecord()

    def encode(thing: SpaceTimeKey, rec: GenericRecord) = {
      rec.put("row", thing.row)
      rec.put("col", thing.col)
      val millis = thing.time.toInstant.getMillis
      val offset = thing.time.getZone.getOffset(millis)
      rec.put("millis", millis)
      rec.put("offset", offset)
    }

    def decode(rec: GenericRecord) = {
      val millis = rec.get("millis").asInstanceOf[Long]
      val offset = rec.get("offset").asInstanceOf[Int]
      val zone = DateTimeZone.forOffsetMillis(offset)
      SpaceTimeKey(
        rec.get("col").asInstanceOf[Int],
        rec.get("row").asInstanceOf[Int],
        new DateTime(millis, zone)
      )
    }
  }

}
