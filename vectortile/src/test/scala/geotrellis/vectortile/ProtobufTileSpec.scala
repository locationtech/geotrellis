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

import geotrellis.vectortile.protobuf.{Protobuf, ProtobufTile}
import org.scalatest._

// --- //

class ProtobufTileSpec extends FunSpec with Matchers {
  describe("onepoint.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/onepoint.mvt"))
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile(Protobuf.decodeIO("vectortile/data/onepoint.mvt"))
      val bytes = Protobuf.encode(tile.asInstanceOf[ProtobufTile].toProtobuf)
      val tile2 = ProtobufTile(Protobuf.decode(bytes))

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("linestring.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/linestring.mvt"))
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile(Protobuf.decodeIO("vectortile/data/linestring.mvt"))
      val bytes = Protobuf.encode(tile.asInstanceOf[ProtobufTile].toProtobuf)
      val tile2 = ProtobufTile(Protobuf.decode(bytes))

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("polygon.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/polygon.mvt"))
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile(Protobuf.decodeIO("vectortile/data/polygon.mvt"))
      val bytes = Protobuf.encode(tile.asInstanceOf[ProtobufTile].toProtobuf)
      val tile2 = ProtobufTile(Protobuf.decode(bytes))

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("roads.mvt") {
    it("must decode") {
      ProtobufTile(Protobuf.decodeIO("vectortile/data/roads.mvt"))
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile(Protobuf.decodeIO("vectortile/data/roads.mvt"))
      val bytes = Protobuf.encode(tile.asInstanceOf[ProtobufTile].toProtobuf)
      val tile2 = ProtobufTile(Protobuf.decode(bytes))

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }
}
