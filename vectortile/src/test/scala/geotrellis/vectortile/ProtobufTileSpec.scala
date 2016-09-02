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

import geotrellis.raster.TileLayout
import geotrellis.spark.SpatialKey
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent
import geotrellis.vectortile.protobuf.ProtobufTile

import org.scalatest._

import java.nio.file.{ Files, Paths }

// --- //

class ProtobufTileSpec extends FunSpec with Matchers {
  val layout = LayoutDefinition(
    Extent(0, 0, 4096, 4096),
    TileLayout(1, 1, 4096, 4096)
  )
  val tileExtent: Extent = layout.mapTransform(SpatialKey(0, 0))

  def read(file: String): Array[Byte] = {
    Files.readAllBytes(Paths.get(file))
  }

  def write(bytes: Array[Byte], file: String): Unit = {
    Files.write(Paths.get(file), bytes)
  }

  describe("onepoint.mvt") {
    it("must decode") {
      ProtobufTile.fromBytes(read("vectortile/data/onepoint.mvt"), tileExtent)
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile.fromBytes(
        read("vectortile/data/onepoint.mvt"),
        tileExtent
      )
      val bytes = tile.asInstanceOf[ProtobufTile].toBytes
      val tile2 = ProtobufTile.fromBytes(bytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("linestring.mvt") {
    it("must decode") {
      ProtobufTile.fromBytes(read("vectortile/data/linestring.mvt"), tileExtent)
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile.fromBytes(
        read("vectortile/data/linestring.mvt"),
        tileExtent
      )
      val bytes = tile.asInstanceOf[ProtobufTile].toBytes
      val tile2 = ProtobufTile.fromBytes(bytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("polygon.mvt") {
    it("must decode") {
      ProtobufTile.fromBytes(
        read("vectortile/data/polygon.mvt"),
        tileExtent
      )
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile.fromBytes(
        read("vectortile/data/polygon.mvt"),
        tileExtent
      )
      val bytes = tile.asInstanceOf[ProtobufTile].toBytes
      val tile2 = ProtobufTile.fromBytes(bytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }

  describe("roads.mvt") {
    it("must decode") {
      ProtobufTile.fromBytes(
        read("vectortile/data/roads.mvt"),
        tileExtent
      )
    }

    it("decode, encode and decode again") {
      val tile = ProtobufTile.fromBytes(
        read("vectortile/data/roads.mvt"),
        tileExtent
      )
      val bytes = tile.asInstanceOf[ProtobufTile].toBytes
      val tile2 = ProtobufTile.fromBytes(bytes, tileExtent)

      tile.layers.keys should equal(tile2.layers.keys)
    }
  }
}
