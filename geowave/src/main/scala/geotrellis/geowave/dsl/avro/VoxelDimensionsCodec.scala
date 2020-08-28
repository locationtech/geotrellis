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

package geotrellis.geowave.dsl.avro

import geotrellis.geowave.dsl._
import geotrellis.store.avro._
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord

trait VoxelDimensionsCodec {
  implicit def voxelDimensions2DCodec: AvroRecordCodec[VoxelDimensions2D] = new AvroRecordCodec[VoxelDimensions2D] {
    def schema = SchemaBuilder
      .record("VoxelDimensions2D").namespace("geotrellis.geowave.dsl")
      .fields()
      .name("width").`type`.intType().noDefault
      .name("height").`type`.intType().noDefault
      .endRecord()

    def encode(bounds: VoxelDimensions2D, rec: GenericRecord): Unit = {
      rec.put("width", bounds.width)
      rec.put("height", bounds.height)
    }

    def decode(rec: GenericRecord): VoxelDimensions2D =
      VoxelDimensions2D(
        rec[Int]("width"),
        rec[Int]("height")
      )
  }

  implicit def voxelDimensions3DCodec: AvroRecordCodec[VoxelDimensions3D] = new AvroRecordCodec[VoxelDimensions3D] {
    def schema = SchemaBuilder
      .record("VoxelDimensions3D").namespace("geotrellis.geowave.dsl")
      .fields()
      .name("width").`type`.intType().noDefault
      .name("height").`type`.intType().noDefault
      .name("depth").`type`.intType().noDefault
      .endRecord()

    def encode(bounds: VoxelDimensions3D, rec: GenericRecord): Unit = {
      rec.put("width", bounds.width)
      rec.put("height", bounds.height)
      rec.put("depth", bounds.depth)
    }

    def decode(rec: GenericRecord): VoxelDimensions3D =
      VoxelDimensions3D(
        rec[Int]("width"),
        rec[Int]("height"),
        rec[Int]("depth")
      )
  }

  implicit def voxelDimensions4DCodec: AvroRecordCodec[VoxelDimensions4D] = new AvroRecordCodec[VoxelDimensions4D] {
    def schema = SchemaBuilder
      .record("VoxelDimensions4D").namespace("com.azavea.api")
      .fields()
      .name("width").`type`.intType().noDefault
      .name("height").`type`.intType().noDefault
      .name("depth").`type`.intType().noDefault
      .name("spissitude").`type`.intType().noDefault
      .endRecord()

    def encode(bounds: VoxelDimensions4D, rec: GenericRecord): Unit = {
      rec.put("width", bounds.width)
      rec.put("height", bounds.height)
      rec.put("depth", bounds.depth)
      rec.put("spissitude", bounds.spissitude)
    }

    def decode(rec: GenericRecord): VoxelDimensions4D =
      VoxelDimensions4D(
        rec[Int]("width"),
        rec[Int]("height"),
        rec[Int]("depth"),
        rec[Int]("spissitude")
      )
  }
}
