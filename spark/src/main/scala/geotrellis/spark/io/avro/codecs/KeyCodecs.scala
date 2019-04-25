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

import geotrellis.tiling.{SpaceTimeKey, SpatialKey}
import geotrellis.spark._
import geotrellis.spark.io.avro._
import org.apache.avro._
import org.apache.avro.generic._

trait KeyCodecs {
  implicit def spatialKeyAvroFormat = new AvroRecordCodec[SpatialKey] {
    def schema = SchemaBuilder
      .record("SpatialKey").namespace("geotrellis.tiling")
      .fields()
      .name("col").`type`().intType().noDefault()
      .name("row").`type`().intType().noDefault()
      .endRecord()

    def encode(key: SpatialKey, rec: GenericRecord) = {
      rec.put("row", key.row)
      rec.put("col", key.col)
    }

    def decode(rec: GenericRecord): SpatialKey =
      SpatialKey(
        rec[Int]("col"),
        rec[Int]("row"))

  }

  implicit def spaceTimeKeyAvroFormat = new AvroRecordCodec[SpaceTimeKey] {
    def schema = SchemaBuilder
      .record("SpaceTimeKey").namespace("geotrellis.tiling")
      .fields()
      .name("col").`type`().intType().noDefault()
      .name("row").`type`().intType().noDefault()
      .name("instant").aliases("millis").`type`().longType().noDefault()
      .endRecord()

    def encode(key: SpaceTimeKey, rec: GenericRecord) = {
      rec.put("row", key.row)
      rec.put("col", key.col)
      rec.put("instant", key.instant)
    }

    def decode(rec: GenericRecord) = {
      SpaceTimeKey(
        rec[Int]("col"),
        rec[Int]("row"),
        rec[Long]("instant")
      )
    }
  }

}

object KeyCodecs extends KeyCodecs
