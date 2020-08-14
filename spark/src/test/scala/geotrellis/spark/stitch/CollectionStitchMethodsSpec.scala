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

package geotrellis.spark.stitch

import geotrellis.layer._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.vector.Extent

import org.scalatest.funspec.AnyFunSpec

class CollectionStitchMethodsSpec extends AnyFunSpec
    with TileBuilders
    with TileLayerRDDBuilders
    with TestEnvironment {

  describe("Stitching spatial collections") {
    it("should correctly stitch back together single band tile collection") {
      val tile =
        createTile(
          Array(
            1, 1,  2, 2,  3, 3,  4, 4,
            1, 1,  2, 2,  3, 3,  4, 4,

            5, 5,  6, 6,  7, 7,  8, 8,
            5, 5,  6, 6,  7, 7,  8, 8,

            9, 9,  10, 10,  11, 11,  12, 12,
            9, 9,  10, 10,  11, 11,  12, 12,

            13, 13,  13, 13,  14, 14,  15, 15,
            13, 13,  13, 13,  14, 14,  15, 15
          ), 8, 8)
      val layer =
        createTileLayerRDD(
          tile,
          TileLayout(2, 2, 4, 4)
        ).toCollection

      assertEqual(tile, layer.stitch.tile)
    }

    it("should correctly stitch back together multi band tile collection") {
      val tile1 =
        createTile(
          Array(
            1, 1,  2, 2,  3, 3,  4, 4,
            1, 1,  2, 2,  3, 3,  4, 4,

            5, 5,  6, 6,  7, 7,  8, 8,
            5, 5,  6, 6,  7, 7,  8, 8,

            9, 9,  10, 10,  11, 11,  12, 12,
            9, 9,  10, 10,  11, 11,  12, 12,

            13, 13,  13, 13,  14, 14,  15, 15,
            13, 13,  13, 13,  14, 14,  15, 15
          ), 8, 8)

      val tile2 =
        createTile(
          Array(
            1, 1,  2, 2,  3, 3,  4, 4,
            1, 1,  2, 2,  3, 3,  4, 4,

            5, 5,  6, 6,  7, 7,  8, 8,
            5, 5,  6, 6,  7, 7,  8, 8,

            9, 9,  10, 10,  11, 11,  12, 12,
            9, 9,  10, 10,  11, 11,  12, 12,

            13, 13,  13, 13,  14, 14,  15, 15,
            13, 13,  13, 13,  14, 14,  15, 15
          ).map(_ * 10), 8, 8)

      val tile = ArrayMultibandTile(tile1, tile2)

      val layer =
        createMultibandTileLayerRDD(
          tile,
          TileLayout(2, 2, 4, 4)
        ).toCollection

      assertEqual(tile, layer.stitch.tile)
    }

    it("should correctly sparse stitch a singleband tile with an offset extent") {
      val expectedTile =
        createTile(
          Array(
            NaN, NaN, NaN, NaN, NaN,
            1, 1,  1, 1, NaN,
            1, 1,  1, 1, NaN,
            1, 1,  1, 1, NaN,
            1, 1,  1, 1, NaN
          ), 5, 5)

      val tile =
        createTile(
          Array(
            1, 1,  1, 1,
            1, 1,  1, 1,
            1, 1,  1, 1,
            1, 1,  1, 1
          ), 4, 4)
      val extent = Extent(0, 0, 4, 4)
      val layer =
        createTileLayerRDD(
          Raster(tile, extent),
          TileLayout(4, 4, 1, 1)
        ).toCollection
      val testExtent = Extent(0, 0, 5, 5)
      assertEqual(layer.sparseStitch(testExtent).get.tile, expectedTile)
    }
  }

  it("should correctly sparse stitch a multiband tile with an offset extent") {
    val expectedTile = ArrayMultibandTile(
      createTile(Array(NaN, NaN, NaN, 1, 1, NaN, 1, 1, NaN), 3, 3),
      createTile(Array(NaN, NaN, NaN, 2, 2, NaN, 2, 2, NaN), 3, 3)
    )
    val tile1 =
      createTile(
        Array(
          1, 1,
          1, 1
        ), 2, 2)
    val tile2 =
      createTile(
        Array(
          2, 2,
          2, 2
        ), 2, 2)
    val tile = ArrayMultibandTile(tile1, tile2)
    val extent = Extent(0, 0, 2, 2)
    val layer =
      createMultibandTileLayerRDD(
        Raster(tile, extent),
        TileLayout(2, 2, 1, 1)
      ).toCollection
    val testExtent = Extent(0, 0, 3, 3)
    assertEqual(layer.sparseStitch(testExtent).get.tile, expectedTile)
  }

  it("should correctly sparse stitch a singleband tile using the raster tile extent") {
    val tile =
      createTile(
        Array(
          1, 1,  1, 1,
          1, 1,  1, 1,
          1, 1,  1, 1,
          1, 1,  1, 1
        ), 4, 4)
    val extent = Extent(0, 0, 4, 4)
    val layer =
      createTileLayerRDD(
        Raster(tile, extent),
        TileLayout(4, 4, 1, 1)
      ).toCollection
    assertEqual(layer.sparseStitch.get.tile, tile)
  }

  it("should correctly sparse stitch an empty collection") {
    val extent = Extent(0, 0, 4, 4)
    val md = TileLayerMetadata(
      IntConstantNoDataCellType,
      LayoutDefinition(extent,TileLayout(4, 4, 1, 1)),
      extent,
      LatLng,
      KeyBounds(SpatialKey(0,0), SpatialKey(3,3))
    )
    val layer = ContextCollection(Seq.empty[(SpatialKey, Tile)], md)

    layer.sparseStitch shouldBe None
  }
}
