package geotrellis.spark.io.avro.codecs

import geotrellis.proj4.CRS
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.vector._

import java.nio.charset.Charset
import java.nio.ByteBuffer

import org.apache.avro._
import org.apache.avro.generic._

// --- //

trait ProjectedExtentCodec {
  implicit def projectedExtentCodec = new AvroRecordCodec[ProjectedExtent] {
    def schema: Schema = SchemaBuilder
      .record("ProjectedExtent").namespace("geotrellis.vector")
      .fields()
      .name("extent").`type`(extentCodec.schema).noDefault()
      .name("proj4").`type`().stringType().noDefault()
      .endRecord()

    def encode(projectedExtent: ProjectedExtent, rec: GenericRecord): Unit = {
      rec.put("extent", extentCodec.encode(projectedExtent.extent))
      rec.put("proj4", projectedExtent.crs.toProj4String)
    }

    def decode(rec: GenericRecord): ProjectedExtent = {
      val proj4 = rec.get("proj4").toString()
      val crs = CRS.fromString(proj4)

      val extent = extentCodec.decode(rec[GenericRecord]("extent"))

      ProjectedExtent(extent, crs)
    }
  }
}
