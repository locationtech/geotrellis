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

trait VoxelBoundsCodec {
  implicit def voxelBounds2DCodec: AvroRecordCodec[VoxelBounds2D] = new AvroRecordCodec[VoxelBounds2D] {
    def schema = SchemaBuilder
      .record("VoxelBounds2D").namespace("geotrellis.geowave.dsl")
      .fields()
      .name("colMin").`type`.intType().noDefault
      .name("colMax").`type`.intType().noDefault
      .name("rowMin").`type`.intType().noDefault
      .name("rowMax").`type`.intType().noDefault
      .endRecord()

    def encode(bounds: VoxelBounds2D, rec: GenericRecord): Unit = {
      rec.put("colMin", bounds.colMin)
      rec.put("colMax", bounds.colMin)
      rec.put("rowMin", bounds.rowMin)
      rec.put("rowMax", bounds.rowMax)
    }

    def decode(rec: GenericRecord): VoxelBounds2D =
      VoxelBounds2D(
        rec[Int]("colMin"),
        rec[Int]("colMax"),
        rec[Int]("rowMin"),
        rec[Int]("rowMax")
      )
  }

  implicit def voxelBounds3DCodec: AvroRecordCodec[VoxelBounds3D] = new AvroRecordCodec[VoxelBounds3D] {
    def schema = SchemaBuilder
      .record("VoxelBounds3D").namespace("geotrellis.geowave.dsl")
      .fields()
      .name("colMin").`type`.intType().noDefault
      .name("colMax").`type`.intType().noDefault
      .name("rowMin").`type`.intType().noDefault
      .name("rowMax").`type`.intType().noDefault
      .name("depthMin").`type`.intType().noDefault
      .name("depthMax").`type`.intType().noDefault
      .endRecord()

    def encode(bounds: VoxelBounds3D, rec: GenericRecord): Unit = {
      rec.put("colMin", bounds.colMin)
      rec.put("colMax", bounds.colMin)
      rec.put("rowMin", bounds.rowMin)
      rec.put("rowMax", bounds.rowMax)
      rec.put("depthMin", bounds.depthMin)
      rec.put("depthMax", bounds.depthMax)
    }

    def decode(rec: GenericRecord): VoxelBounds3D =
      VoxelBounds3D(
        rec[Int]("colMin"),
        rec[Int]("colMax"),
        rec[Int]("rowMin"),
        rec[Int]("rowMax"),
        rec[Int]("depthMin"),
        rec[Int]("depthMax")
      )
  }

  implicit def voxelBounds4DCodec: AvroRecordCodec[VoxelBounds4D] = new AvroRecordCodec[VoxelBounds4D] {
    def schema = SchemaBuilder
      .record("VoxelBounds4D").namespace("geotrellis.geowave.dsl")
      .fields()
      .name("colMin").`type`.intType().noDefault
      .name("colMax").`type`.intType().noDefault
      .name("rowMin").`type`.intType().noDefault
      .name("rowMax").`type`.intType().noDefault
      .name("depthMin").`type`.intType().noDefault
      .name("depthMax").`type`.intType().noDefault
      .name("spissitudeMin").`type`.intType().noDefault
      .name("spissitudeMax").`type`.intType().noDefault
      .endRecord()

    def encode(bounds: VoxelBounds4D, rec: GenericRecord): Unit = {
      rec.put("colMin", bounds.colMin)
      rec.put("colMax", bounds.colMin)
      rec.put("rowMin", bounds.rowMin)
      rec.put("rowMax", bounds.rowMax)
      rec.put("depthMin", bounds.depthMin)
      rec.put("depthMax", bounds.depthMax)
      rec.put("spissitudeMin", bounds.spissitudeMin)
      rec.put("spissitudeMax", bounds.spissitudeMax)
    }

    def decode(rec: GenericRecord): VoxelBounds4D =
      VoxelBounds4D(
        rec[Int]("colMin"),
        rec[Int]("colMax"),
        rec[Int]("rowMin"),
        rec[Int]("rowMax"),
        rec[Int]("depthMin"),
        rec[Int]("depthMax"),
        rec[Int]("spissitudeMin"),
        rec[Int]("spissitudeMax")
      )
  }
}
