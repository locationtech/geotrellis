package geotrellis.spark.io.avro.codecs

import geotrellis.proj4.CRS
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.vector._

import org.apache.avro._
import org.apache.avro.generic._

// --- //

trait ProjectedExtentCodec {
  implicit def projectedExtentCodec = new AvroRecordCodec[ProjectedExtent] {
    def schema: Schema = SchemaBuilder
      .record("ProjectedExtent").namespace("geotrellis.spark")
      .fields()
      .name("extent").`type`(extentCodec.schema).noDefault()
      .name("epsg").`type`().intType().noDefault()
      .endRecord()

    def encode(projectedExtent: ProjectedExtent, rec: GenericRecord): Unit = {
      rec.put("extent", extentCodec.encode(projectedExtent.extent))
      rec.put("epsg", projectedExtent.crs.epsgCode.get)
    }

    def decode(rec: GenericRecord): ProjectedExtent = {
      val epsg = rec[Int]("epsg")
      val crs = CRS.fromEpsgCode(epsg)

      val extent = extentCodec.decode(rec[GenericRecord]("extent"))

      ProjectedExtent(extent, crs)
    }
  }
}
