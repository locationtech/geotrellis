package geotrellis.spark.io.avro.codecs

import geotrellis.spark.io.avro._
import geotrellis.spark.{SpaceTimeKey, SpatialKey}
import org.apache.avro._
import org.apache.avro.generic._
import org.joda.time.{DateTime, DateTimeZone}

trait KeyCodecs {
  implicit def spatialKeyAvroFormat = new AvroRecordCodec[SpatialKey] {
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
        rec[Int]("col"),
        rec[Int]("row"))

  }

  implicit def spaceTimeKeyAvroFormat = new AvroRecordCodec[SpaceTimeKey] {
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
      val millis = rec[Long]("millis")
      val offset = rec[Int]("offset")
      val zone = DateTimeZone.forOffsetMillis(offset)
      SpaceTimeKey(
        rec[Int]("col"),
        rec[Int]("row"),
        new DateTime(millis, zone)
      )
    }
  }

}

object KeyCodecs extends KeyCodecs
