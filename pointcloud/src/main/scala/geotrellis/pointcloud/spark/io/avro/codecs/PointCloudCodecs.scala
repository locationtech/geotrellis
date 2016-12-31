/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.pointcloud.spark.io.avro.codecs

import geotrellis.spark.io.avro._

import io.pdal._
import org.apache.avro._
import org.apache.avro.generic._
import org.apache.avro.util.Utf8

import java.util
import java.nio.ByteBuffer
import scala.collection.JavaConversions._

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
        rec[Utf8]("id").toString,
        rec[Utf8]("type").toString,
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
      val dimTypes = new util.HashMap[String, GenericRecord]()
      pc.dimTypes.foreach { case (k, v) => dimTypes += k -> sizedDimTypeCodec.encode(v) }

      rec.put("dimTypes", dimTypes)
    }

    def decode(rec: GenericRecord): PointCloud = {
      val dimTypes = new util.HashMap[String, SizedDimType]()
      rec[util.Map[Utf8, GenericRecord]]("dimTypes")
        .foreach { case (k, v) => dimTypes += k.toString -> sizedDimTypeCodec.decode(v) }

      PointCloud(
        rec[ByteBuffer]("bytes").array,
        dimTypes
      )
    }
  }
}
