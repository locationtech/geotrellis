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

import geotrellis.raster._
import geotrellis.spark.io.avro._

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic._

trait TileFeatureCodec {

  implicit def tileFeatureCodec[
    T <: Tile: AvroRecordCodec,
    D: AvroRecordCodec
  ]: AvroRecordCodec[TileFeature[T, D]] = new AvroRecordCodec[TileFeature[T, D]] {
    def schema = SchemaBuilder
      .record("TileFeature").namespace("geotrellis.raster")
      .fields()
      .name("tile").`type`(implicitly[AvroRecordCodec[T]].schema).noDefault
      .name("data").`type`(implicitly[AvroRecordCodec[D]].schema).noDefault
      .endRecord()

    def encode(tileFeature: TileFeature[T, D], rec: GenericRecord): Unit = {
      rec.put("tile", implicitly[AvroRecordCodec[T]].encode(tileFeature.tile))
      rec.put("data", implicitly[AvroRecordCodec[D]].encode(tileFeature.data))
    }

    def decode(rec: GenericRecord): TileFeature[T,D] = {
      val tile: T = implicitly[AvroRecordCodec[T]].decode(rec.get("tile").asInstanceOf[GenericRecord])
      val data: D = implicitly[AvroRecordCodec[D]].decode(rec.get("data").asInstanceOf[GenericRecord])
      TileFeature(tile, data)
    }
  }

  implicit def multibandTileFeatureCodec[
    T <: MultibandTile: AvroRecordCodec,
    D: AvroRecordCodec
  ]: AvroRecordCodec[TileFeature[T, D]] = new AvroRecordCodec[TileFeature[T, D]] {
    def schema = SchemaBuilder
      .record("TileFeature").namespace("geotrellis.raster")
      .fields()
      .name("tile").`type`(implicitly[AvroRecordCodec[T]].schema).noDefault
      .name("data").`type`(implicitly[AvroRecordCodec[D]].schema).noDefault
      .endRecord()

    def encode(tileFeature: TileFeature[T, D], rec: GenericRecord): Unit = {
      rec.put("tile", implicitly[AvroRecordCodec[T]].encode(tileFeature.tile))
      rec.put("data", implicitly[AvroRecordCodec[D]].encode(tileFeature.data))
    }

    def decode(rec: GenericRecord): TileFeature[T,D] = {
      val tile: T = implicitly[AvroRecordCodec[T]].decode(rec.get("tile").asInstanceOf[GenericRecord])
      val data: D = implicitly[AvroRecordCodec[D]].decode(rec.get("data").asInstanceOf[GenericRecord])
      TileFeature(tile, data)
    }
  }
}
