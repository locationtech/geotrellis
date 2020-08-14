/*
 * Copyright 2019 Azavea
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

package geotrellis.vectortile

import geotrellis.vector.{Extent, Point}

import java.nio.file.{Files, Paths}

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

// --- //

class ProtobufTileSpec extends AnyFunSpec with Matchers {
  val tileExtent: Extent = Extent(0, 0, 4096, 4096)

  def read(file: String): Array[Byte] = {
    Files.readAllBytes(Paths.get(file))
  }

  def write(bytes: Array[Byte], file: String): Unit = {
    Files.write(Paths.get(file), bytes)
  }

  describe("onepoint.mvt") {
    it("must decode") {
      VectorTile.fromBytes(read("vectortile/data/onepoint.mvt"), tileExtent)
    }

    it("should encode ids into the tile") {
      val layerId = "x"
      val mvtFeature = MVTFeature(Some(1), Point(10, 10), Map.empty)
      val layer = StrictLayer(layerId, 4096, 2, tileExtent,
                              Seq(mvtFeature), Seq(), Seq(), Seq(), Seq(), Seq())
      val tile = VectorTile(Map(layerId -> layer), tileExtent)
      val tile2 = VectorTile.fromBytes(tile.toBytes, tileExtent)
      val tileId = tile.layers(layerId).points.head.id
      val tile2Id = tile2.layers(layerId).points.head.id
      tileId should equal(tile2Id)
    }

    it("should encode attributes into the tile") {
      val layerId = "x"
      val mvtFeature = MVTFeature(Some(1), Point(10, 10), Map(
        "building" -> VString("yes"),
        "isValid" -> VBool(true),
        "elevation" -> VDouble(100.5),
        "population" -> VInt64(6)
      ))
      val layer = StrictLayer(layerId, 4096, 2, tileExtent,
                              Seq(mvtFeature), Seq(), Seq(), Seq(), Seq(), Seq())
      val tile = VectorTile(Map(layerId -> layer), tileExtent)
      val tile2 = VectorTile.fromBytes(tile.toBytes, tileExtent)
      val tileId = tile.layers(layerId).points.head.data.toList.foreach {
        case (key, value) => {
          tile2.layers(layerId).points.head.data(key) should equal(value)
        }
      }
    }

    it("decode, encode and decode again") {
      val tile: VectorTile = VectorTile.fromBytes(read("vectortile/data/onepoint.mvt"), tileExtent)
      val tile2 = VectorTile.fromBytes(tile.toBytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("linestring.mvt") {
    it("must decode") {
      VectorTile.fromBytes(read("vectortile/data/linestring.mvt"), tileExtent)
    }

    it("decode, encode and decode again") {
      val tile = VectorTile.fromBytes(read("vectortile/data/linestring.mvt"), tileExtent)
      val tile2 = VectorTile.fromBytes(tile.toBytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("polygon.mvt") {
    it("must decode") {
      VectorTile.fromBytes(read("vectortile/data/polygon.mvt"), tileExtent)
    }

    it("decode, encode and decode again") {
      val tile = VectorTile.fromBytes(read("vectortile/data/polygon.mvt"), tileExtent)
      val tile2 = VectorTile.fromBytes(tile.toBytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("roads.mvt") {
    it("must decode") {
      VectorTile.fromBytes(read("vectortile/data/roads.mvt"), tileExtent)
    }

    it("decode, encode and decode again") {
      val tile = VectorTile.fromBytes(read("vectortile/data/roads.mvt"), tileExtent)
      val tile2 = VectorTile.fromBytes(tile.toBytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }
}
