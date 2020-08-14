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

package geotrellis.layer

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector._
import _root_.io.circe.syntax._
import _root_.io.circe.parser.decode

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class TileLayerMetadataSpec extends AnyFunSpec with Matchers {
  describe("TileLayerMetadata JSON codecs") {

    val cellType = DoubleCellType
    val layoutDefinition = LayoutDefinition(Extent(-126.0, 21.608163265306125, -62.0, 54.0),
                                            TileLayout(4, 2, 256, 256))
    val extent = Extent(-126.0, 23.0, -66.0, 54.0)
    val bounds = KeyBounds(SpaceTimeKey(0,0,0), SpaceTimeKey(3,1,1000000))
    val tileLayerMetadata: TileLayerMetadata[SpaceTimeKey] = TileLayerMetadata(
      cellType,
      layoutDefinition,
      extent,
      LatLng,
      bounds
    )

    it("should convert TileLayerMetadata to json and back with specific JSON keys") {
      val json = tileLayerMetadata.asJson
      json.hcursor.downField("cellType").as[CellType] should equal(Right(cellType))
      json.hcursor.downField("layoutDefinition").as[LayoutDefinition] should equal(Right(layoutDefinition))
      json.hcursor.downField("extent").as[Extent] should equal(Right(extent))
      json.hcursor.downField("crs").as[CRS] should equal(Right(LatLng))
      json.hcursor.downField("bounds").as[Bounds[SpaceTimeKey]] should equal(Right(bounds))

      json.as[TileLayerMetadata[SpaceTimeKey]].right.map(_ should equal(tileLayerMetadata))
    }

    it("should parse a GeoTrellis 1.2 TileLayerMetadata object") {
      // NOTE: If this test breaks because the keys in TileLayerMetadata have changed, then
      //       we've broken backwards compatibility in reading TileLayerMetadata in our layers.
      val metadata: String = "{\"extent\":{\"xmin\":-126.0,\"ymin\":23.0,\"xmax\":-66.0,\"ymax\":54.0},\"layoutDefinition\":{\"extent\":{\"xmin\":-126.0,\"ymin\":21.608163265306125,\"xmax\":-62.0,\"ymax\":54.0},\"tileLayout\":{\"layoutCols\":4,\"layoutRows\":2,\"tileCols\":256,\"tileRows\":256}},\"bounds\":{\"minKey\":{\"col\":0,\"row\":0,\"instant\":1514764800000},\"maxKey\":{\"col\":3,\"row\":1,\"instant\":4133894400000}},\"cellType\":\"float32ud1.0000000150474662E30\",\"crs\":\"+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs \"}"
      val result = decode[TileLayerMetadata[SpaceTimeKey]](metadata)
      result.isRight should equal(true)
    }
  }
}

