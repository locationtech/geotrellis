/*
 * Copyright 2017 Azavea
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
import geotrellis.spark.io.avro.{AvroRecordCodec, _}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.apache.avro.{Schema, SchemaBuilder}

trait CRSCodec {
  implicit def crsCodec = new AvroRecordCodec[CRS] {
    override def schema: Schema = SchemaBuilder
      .record("CRS").namespace("geotrellis.spark")
      .fields()
      .name("epsg").`type`().optional().intType()
      .name("proj4").`type`().optional().stringType()
      .endRecord()

    override def encode(crs: CRS, rec: GenericRecord): Unit = {
      if(crs.epsgCode.isDefined) {
        rec.put("epsg", crs.epsgCode.get)
      }
      else {
        rec.put("proj4", crs.toProj4String)
      }
    }
    override def decode(rec: GenericRecord): CRS = {
      if(rec[AnyRef]("epsg") != null) {
        val epsg = rec[Int]("epsg")
        CRS.fromEpsgCode(epsg)
      }
      else {
        val proj4 = rec[Utf8]("proj4")
        CRS.fromString(proj4.toString)
      }
    }
  }
}
