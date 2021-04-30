/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.adapter.raster.avro

import geotrellis.raster.{CellGrid, Raster}
import geotrellis.store.avro._
import geotrellis.vector.Extent

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord

trait RasterCodec {
  implicit def rasterCodec[T <: CellGrid[Int]: AvroRecordCodec]: AvroRecordCodec[Raster[T]] = new AvroRecordCodec[Raster[T]] {
    def schema = SchemaBuilder
      .record("Raster").namespace("geotrellis.raster")
      .fields()
      .name("tile").`type`(AvroRecordCodec[T].schema).noDefault
      .name("extent").`type`(AvroRecordCodec[Extent].schema).noDefault
      .endRecord()

    def encode(raster: Raster[T], rec: GenericRecord): Unit = {
      rec.put("tile", AvroRecordCodec[T].encode(raster.tile))
      rec.put("extent", AvroRecordCodec[Extent].encode(raster.extent))
    }

    def decode(rec: GenericRecord): Raster[T] = {
      val tile = AvroRecordCodec[T].decode(rec[GenericRecord]("tile"))
      val extent = AvroRecordCodec[Extent].decode(rec[GenericRecord]("extent"))
      Raster(tile, extent)
    }
  }

}
