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
import com.vividsolutions.jts.geom.Coordinate

import java.util
import java.nio.ByteBuffer
import scala.collection.JavaConverters._

trait PointCloudCodecs {
  implicit def coordinateCodec = new AvroRecordCodec[Coordinate] {
    def schema: Schema = SchemaBuilder
      .record("Coordinate").namespace("com.vividsolutions.jts.geom")
      .fields()
      .name("x").`type`().doubleType().noDefault()
      .name("y").`type`().doubleType().noDefault()
      .name("z").`type`().doubleType().noDefault()
      .endRecord()

    def encode(c: Coordinate, rec: GenericRecord): Unit = {
      rec.put("x", c.x)
      rec.put("y", c.y)
      rec.put("z", c.z)
    }

    def decode(rec: GenericRecord): Coordinate =
      new Coordinate(
        rec[Double]("x"),
        rec[Double]("y"),
        rec[Double]("z")
      )
  }

  implicit def arrayCoordinateCodec = new AvroRecordCodec[Array[Coordinate]] {
    def schema: Schema = SchemaBuilder
      .record("ArrayCoordinate").namespace("com.vividsolutions.jts.geom")
      .fields()
      .name("arr").`type`.array().items.`type`(coordinateCodec.schema).noDefault()
      .endRecord()

    def encode(arr: Array[Coordinate], rec: GenericRecord): Unit = {
      rec.put("arr", java.util.Arrays.asList(arr.map(coordinateCodec.encode):_*))
    }

    def decode(rec: GenericRecord): Array[Coordinate] =
      rec.get("arr")
        .asInstanceOf[java.util.Collection[GenericRecord]]
        .asScala
        .map(coordinateCodec.decode)
        .toArray
  }

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
      pc.dimTypes.asScala.foreach { case (k, v) => dimTypes.put(k, sizedDimTypeCodec.encode(v)) }

      rec.put("dimTypes", dimTypes)
    }

    def decode(rec: GenericRecord): PointCloud = {
      val dimTypes = new util.HashMap[String, SizedDimType]()
      rec[util.Map[Utf8, GenericRecord]]("dimTypes")
        .asScala
        .foreach { case (k, v) => dimTypes.put(k.toString, sizedDimTypeCodec.decode(v)) }

      PointCloud(
        rec[ByteBuffer]("bytes").array,
        dimTypes
      )
    }
  }
}
