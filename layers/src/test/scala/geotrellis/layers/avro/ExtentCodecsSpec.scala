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

import geotrellis.layers.avro.codecs.Implicits._
import org.scalatest._
import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.tiling._

class ExtentCodecsSpec extends FunSpec with Matchers with AvroTools  {
  val fortyThreeTwentyFour = CRS.fromEpsgCode(4324)
  // A proj4 CRS known to not have a CRS code.
  val nonEpsgCrs = CRS.fromString("+proj=sinu +lon_0=0.0 +x_0=0.0 +y_0=0.0 +a=6371007.181 +b=6371007.181 +units=m")

  describe("ExtentCodecs") {
    it("encodes Extent"){
      roundTrip(Extent(0, 1, 2, 3))
    }
    it("encodes ProjectedExtent") {
      roundTrip(ProjectedExtent(Extent(0, 1, 2, 3), fortyThreeTwentyFour))
    }
    it("encodes ProjectedExtent with missing EPSG code"){
      assert(nonEpsgCrs.epsgCode.isEmpty)
      roundTrip(ProjectedExtent(Extent(0, 1, 2, 3), nonEpsgCrs))
    }
    it("encodes TemporalProjectedExtent"){
      roundTrip(TemporalProjectedExtent(Extent(0, 1, 2, 3), fortyThreeTwentyFour, 1.toLong))
    }
    it("encodes TemporalProjectedExtent with missing EPSG code"){
      assert(nonEpsgCrs.epsgCode.isEmpty)
      roundTrip(TemporalProjectedExtent(Extent(0, 1, 2, 3), nonEpsgCrs, 1.toLong))
    }
  }
}
