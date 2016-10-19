package geotrellis.spark.io.avro.codecs

import geotrellis.spark.io.avro._
import geotrellis.vector.Extent

import org.apache.avro._
import org.apache.avro.generic._

// --- //

trait ExtentCodec {
  implicit def extentCodec = new AvroRecordCodec[Extent] {
    def schema: Schema = SchemaBuilder
      .record("Extent").namespace("geotrellis.spark")
      .fields()
      .name("xmin").`type`().doubleType().noDefault()
      .name("ymin").`type`().doubleType().noDefault()
      .name("xmax").`type`().doubleType().noDefault()
      .name("ymax").`type`().doubleType().noDefault()
      .endRecord()

    def encode(extent: Extent, rec: GenericRecord): Unit = {
      rec.put("xmin", extent.xmin)
      rec.put("ymin", extent.ymin)
      rec.put("xmax", extent.xmax)
      rec.put("ymax", extent.ymax)
    }

    def decode(rec: GenericRecord): Extent = {
      Extent(
        rec[Double]("xmin"),
        rec[Double]("ymin"),
        rec[Double]("xmax"),
        rec[Double]("ymax")
      )
    }
  }
}
