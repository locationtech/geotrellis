package geotrellis.pointcloud.spark.io.avro.codecs

import geotrellis.spark.io.avro._

import io.pdal._
import org.apache.avro._
import org.apache.avro.generic._

import java.util
import java.nio.ByteBuffer
import scala.collection.JavaConverters._

trait PointCloudCodecs {

  implicit def dimTypeCodec = new AvroRecordCodec[DimType] {
    def schema: Schema = SchemaBuilder
      .record("DimType").namespace("io.pdal")
      .fields()
      .name("id").`type`().stringType().noDefault()
      .name("type").`type`().stringType().noDefault()
      .name("scale").`type`().doubleType().noDefault()
      .name("offset").`type`().doubleType().noDefault()
      .endRecord()

    def encode(dt: DimType, rec: GenericRecord): Unit = {
      rec.put("id", dt.id)
      rec.put("type", dt.`type`)
      rec.put("scale", dt.scale)
      rec.put("offset", dt.offset)
    }

    def decode(rec: GenericRecord): DimType =
      DimType(
        rec[String]("id"),
        rec[String]("type"),
        rec[Double]("scale"),
        rec[Double]("offset")
      )
  }

  implicit def sizedDimTypeCodec = new AvroRecordCodec[SizedDimType] {
    def schema: Schema = SchemaBuilder
      .record("SizedDimType").namespace("io.pdal")
      .fields()
      .name("dimType").`type`(dimTypeCodec.schema).noDefault()
      .name("size").`type`().longType().noDefault()
      .name("offset").`type`().longType().noDefault()
      .endRecord()

    def encode(sdt: SizedDimType, rec: GenericRecord): Unit = {
      rec.put("dimType", dimTypeCodec.encode(sdt.dimType))
      rec.put("size", sdt.size)
      rec.put("offset", sdt.offset)
    }

    def decode(rec: GenericRecord): SizedDimType =
      SizedDimType(
        dimTypeCodec.decode(rec[GenericRecord]("dimType")),
        rec[Long]("size"),
        rec[Long]("offset")
      )
  }

  implicit def pointCloudCodec = new AvroRecordCodec[PointCloud] {
    def schema: Schema = SchemaBuilder
      .record("PointCloud").namespace("io.pdal")
      .fields()
      .name("bytes").`type`().bytesType().noDefault()
      .name("dimTypes").`type`().map().values(sizedDimTypeCodec.schema).noDefault()
      .endRecord()

    def encode(pc: PointCloud, rec: GenericRecord): Unit = {
      rec.put("bytes", ByteBuffer.wrap(pc.bytes))
      rec.put("dimTypes", pc.dimTypes.asScala.map { case (k, v) => k -> sizedDimTypeCodec.encode(v) }.asJava)
    }

    def decode(rec: GenericRecord): PointCloud =
      PointCloud(
        rec[ByteBuffer]("bytes").array,
        rec[util.Map[String, GenericRecord]]("dimTypes")
          .asScala
          .map { case (k, v) => k -> sizedDimTypeCodec.decode(v) }
          .asJava
      )
  }

}
