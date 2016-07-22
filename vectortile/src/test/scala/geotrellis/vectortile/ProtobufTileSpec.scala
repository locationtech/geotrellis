/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vectortile

import geotrellis.vectortile.protobuf.{ Protobuf, ProtobufTile }
import org.scalatest._

// --- //

class ProtobufTileSpec extends FunSpec with Matchers {
  describe("onepoint.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/onepoint.mvt"))
    }
  }

  describe("linestring.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/linestring.mvt"))
    }
  }

  describe("polygon.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/polygon.mvt"))
    }
  }

  describe("roads.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/roads.mvt"))
    }
  }


}
