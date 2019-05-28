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

package geotrellis.layers.avro

import geotrellis.layers.avro.codecs.{KeyValueRecordCodec, TileCodecs, KeyCodecs}
import TileCodecs._
import KeyCodecs._
import org.scalatest._
import geotrellis.raster._
import geotrellis.tiling._

class TileRecordSpec extends FunSpec with AvroTools with Matchers {
  describe("TileRecordCodecs") {
    it("encodes (key,tile) pairs"){
      val pairs = Vector(
        SpatialKey(1,2) -> DoubleArrayTile.fill(1,10,12),
        SpatialKey(3,6) -> DoubleArrayTile.fill(2,10,12),
        SpatialKey(3,4) -> DoubleArrayTile.fill(3,10,12)
      )

      val codec = new KeyValueRecordCodec[SpatialKey, DoubleArrayTile]

      roundTrip(pairs)(codec)
    }
  }
}
