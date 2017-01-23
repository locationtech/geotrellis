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

package geotrellis.spark.io.avro

import geotrellis.spark.io.avro.codecs._
import org.scalatest._
import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io.avro.AvroTools._

class ExtentCodecsSpec extends FunSpec with Matchers with AvroTools  {
  describe("ExtentCodecs") {
    it("encodes Extent"){
      roundTrip(Extent(0, 1, 2, 3))
    }
    it("encodes ProjectedExtent") {
      roundTrip(ProjectedExtent(Extent(0, 1, 2, 3), CRS.fromEpsgCode(4324)))
    }
    it("encodes TemporalProjectedExtent"){
      roundTrip(TemporalProjectedExtent(Extent(0, 1, 2, 3), CRS.fromEpsgCode(4324), 1.toLong))
    }
  }
}
