package geotrellis.spark.io.avro.codecs

import geotrellis.spark.io.avro._
import geotrellis.spark.{GridTimeKey, GridKey}
import org.apache.avro._
import org.apache.avro.generic._
import org.joda.time.{DateTime, DateTimeZone}

trait KeyCodecs {
  implicit def spatialKeyAvroFormat = new AvroRecordCodec[GridKey] {
    def schema = SchemaBuilder
      .record("GridKey").namespace("geotrellis.spark")
      .fields()
      .name("col").`type`().intType().noDefault()
      .name("row").`type`().intType().noDefault()
      .endRecord()

    def encode(key: GridKey, rec: GenericRecord) = {
      rec.put("row", key.row)
      rec.put("col", key.col)
    }

    def decode(rec: GenericRecord): GridKey =
      GridKey(
        rec[Int]("col"),
        rec[Int]("row"))

  }

  implicit def spaceTimeKeyAvroFormat = new AvroRecordCodec[GridTimeKey] {
    def schema = SchemaBuilder
      .record("GridTimeKey").namespace("geotrellis.spark")
      .fields()
      .name("col").`type`().intType().noDefault()
      .name("row").`type`().intType().noDefault()
      .name("instant").aliases("millis").`type`().longType().noDefault()
      .endRecord()

    def encode(key: GridTimeKey, rec: GenericRecord) = {
      rec.put("row", key.row)
      rec.put("col", key.col)
      rec.put("instant", key.instant)
    }

    def decode(rec: GenericRecord) = {
      GridTimeKey(
        rec[Int]("col"),
        rec[Int]("row"),
        rec[Long]("instant")
      )
    }
  }

}

object KeyCodecs extends KeyCodecs
