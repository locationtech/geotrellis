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

import geotrellis.tiling.TemporalProjectedExtent
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.Implicits._
import org.apache.avro._
import org.apache.avro.generic._


trait TemporalProjectedExtentCodec {
  implicit def temporalProjectedExtentCodec = new AvroRecordCodec[TemporalProjectedExtent] {
    def schema: Schema = {
      val base = SchemaBuilder
        .record("TemporalProjectedExtent").namespace("geotrellis.spark")
        .fields()
        .name("extent").`type`(extentCodec.schema).noDefault()

      injectFields(crsCodec.schema, base)
        .name("instant").`type`().longType().noDefault()
        .endRecord()
    }

    def encode(temporalProjectedExtent: TemporalProjectedExtent, rec: GenericRecord): Unit = {
      rec.put("extent", extentCodec.encode(temporalProjectedExtent.extent))
      crsCodec.encode(temporalProjectedExtent.crs, rec)
      rec.put("instant", temporalProjectedExtent.instant)
    }

    def decode(rec: GenericRecord): TemporalProjectedExtent = {
      val instant = rec[Long]("instant")
      val crs = crsCodec.decode(rec)
      val extent = extentCodec.decode(rec[GenericRecord]("extent"))

      TemporalProjectedExtent(extent, crs, instant)
    }
  }
}

