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

    def encode(key: SpatialKey, rec: GenericRecord) = {
      rec.put("row", key.row)
      rec.put("col", key.col)
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
      .name("instant").aliases("millis").`type`().longType().noDefault()
      .endRecord()

    def encode(key: SpaceTimeKey, rec: GenericRecord) = {
      rec.put("row", key.row)
      rec.put("col", key.col)
      rec.put("instant", key.instant)
    }

    def decode(rec: GenericRecord) = {
      SpaceTimeKey(
        rec[Int]("col"),
        rec[Int]("row"),
        rec[Long]("instant")
      )
    }
  }

}

object KeyCodecs extends KeyCodecs
