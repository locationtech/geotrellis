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

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.vector._
import org.apache.avro._
import org.apache.avro.generic._

// --- //

trait ProjectedExtentCodec {
  implicit def projectedExtentCodec = new AvroRecordCodec[ProjectedExtent] {
    def schema: Schema = {
      val base = SchemaBuilder
        .record("ProjectedExtent").namespace("geotrellis.vector")
        .fields()
        .name("extent").`type`(extentCodec.schema).noDefault()

      injectFields(crsCodec.schema, base)
        .endRecord()
    }

    def encode(projectedExtent: ProjectedExtent, rec: GenericRecord): Unit = {
      rec.put("extent", extentCodec.encode(projectedExtent.extent))
      crsCodec.encode(projectedExtent.crs, rec)
    }

    def decode(rec: GenericRecord): ProjectedExtent = {
      val crs = crsCodec.decode(rec)
      val extent = extentCodec.decode(rec[GenericRecord]("extent"))

      ProjectedExtent(extent, crs)
    }
  }
}
