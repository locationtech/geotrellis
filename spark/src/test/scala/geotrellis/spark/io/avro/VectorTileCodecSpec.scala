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

import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.raster.TileLayout
import geotrellis.spark.SpatialKey
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent
import geotrellis.vectortile._

import org.scalatest._

import java.nio.file.{ Files, Paths }

// --- //

class VectorTileCodecSpec extends FunSpec with Matchers {

  val layout = LayoutDefinition(
    Extent(0, 0, 4096, 4096),
    TileLayout(1, 1, 4096, 4096)
  )
  val tileExtent: Extent = layout.mapTransform(SpatialKey(0, 0))

  def read(file: String): Array[Byte] = {
    Files.readAllBytes(Paths.get(file))
  }

  describe("Avro Codec Isomorphism") {
    it("onepoint.mvt") {
      val bytes = read("vectortile/data/onepoint.mvt")
      val tile = VectorTile.fromBytes(bytes, tileExtent)

      vectorTileCodec.decode(vectorTileCodec.encode(tile)) shouldBe tile
      tile.toBytes shouldBe bytes
    }

    it("linestring.mvt") {
      val bytes = read("vectortile/data/linestring.mvt")
      val tile = VectorTile.fromBytes(bytes, tileExtent)

      vectorTileCodec.decode(vectorTileCodec.encode(tile)) shouldBe tile
      tile.toBytes shouldBe bytes
    }

    it("polygon.mvt") {
      val bytes = read("vectortile/data/polygon.mvt")
      val tile = VectorTile.fromBytes(bytes, tileExtent)

      vectorTileCodec.decode(vectorTileCodec.encode(tile)) shouldBe tile
      tile.toBytes shouldBe bytes
    }

    /* This test does not check for byte-to-byte equivalence, as that
     * will never happen with our codec. Since Feature-level metadata
     * is stored as a Map, when reencoding these to Protobuf data,
     * the key/value pairs are rewritten in an arbitrary order.
     */
    it("roads.mvt") {
      val bytes = read("vectortile/data/roads.mvt")
      val tile = VectorTile.fromBytes(bytes, tileExtent)

      val decoded = vectorTileCodec.decode(vectorTileCodec.encode(tile))

      decoded.layers.keys should equal(tile.layers.keys)
      decoded.layers.values.map(_.points) should equal(tile.layers.values.map(_.points))
      decoded.layers.values.map(_.multiPoints) should equal(tile.layers.values.map(_.multiPoints))
      decoded.layers.values.map(_.lines) should equal(tile.layers.values.map(_.lines))
      decoded.layers.values.map(_.multiLines) should equal(tile.layers.values.map(_.multiLines))
      decoded.layers.values.map(_.polygons) should equal(tile.layers.values.map(_.polygons))
//      decoded.layers.values.map(_.multiPolygons) should equal(tile.layers.values.map(_.multiPolygons))
    }
  }
}
