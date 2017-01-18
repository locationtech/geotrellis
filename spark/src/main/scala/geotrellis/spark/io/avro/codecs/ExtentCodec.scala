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

package geotrellis.spark.io.avro.codecs

import geotrellis.proj4.CRS
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.vector._

import org.apache.avro._
import org.apache.avro.generic._

// --- //

trait ExtentCodecs {
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

  implicit def projectedExtentCodec = new AvroRecordCodec[ProjectedExtent] {
    def schema: Schema = SchemaBuilder
      .record("ProjectedExtent").namespace("geotrellis.spark")
      .fields()
      .name("extent").`type`(extentCodec.schema).noDefault()
      .name("wkt").`type`().stringType().noDefault()
      .endRecord()

    def encode(projectedExtent: ProjectedExtent, rec: GenericRecord): Unit = {
      rec.put("extent", projectedExtent.extent)
      rec.put("wkt", projectedExtent.crs)
    }

    def decode(rec: GenericRecord): ProjectedExtent = {
      val wktString = rec[String]("crs")
      val crs = CRS.fromWKT(wktString)

      val extent = extentCodec.decode(rec("extent"))

      ProjectedExtent(extent, crs)
    }
  }

  implicit def temporalProjectedExtentCodec = new AvroRecordCodec[TemporalProjectedExtent] {
    def schema: Schema = SchemaBuilder
      .record("TemporalProjectedExtent").namespace("geotrellis.spark")
      .fields()
      .name("extent").`type`(extentCodec.schema).noDefault()
      .name("wkt").`type`().stringType().noDefault()
      .name("instant").`type`().longType().noDefault()
      .endRecord()

    def encode(temporalProjectedExtent: TemporalProjectedExtent, rec: GenericRecord): Unit = {
      rec.put("extent", temporalProjectedExtent.extent)
      rec.put("wkt", temporalProjectedExtent.crs)
      rec.put("instant", temporalProjectedExtent.instant)
    }

    def decode(rec: GenericRecord): TemporalProjectedExtent = {
      val instant = rec[Long]("instant")
      val wktString = rec[String]("wkt")
      val crs = CRS.fromWKT(wktString)

      val extent = extentCodec.decode(rec("extent"))

      TemporalProjectedExtent(extent, crs, instant)
    }
  }
}

object ExtentCodecs extends ExtentCodecs
