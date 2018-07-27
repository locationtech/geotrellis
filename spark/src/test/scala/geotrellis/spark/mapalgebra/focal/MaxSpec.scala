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

package geotrellis.spark.mapalgebra.focal

import geotrellis.spark._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class MaxSpec extends FunSpec with TestEnvironment {

  describe("Max Focal Spec") {

    val nd = NODATA

    it("should square max for raster rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, nd,1,   8, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMax(Square(1)).stitch.tile.toArray

      val expected = Array(
        9, 9, 7,    2, 2, 2,    3, 3, 3,
        9, 9, 8,    3, 3, 3,    3, 3, 3,

        9, 9, 8,    7, 3, 8,    8, 8, 3,
        8, 8, 8,    7, 3, 8,    8, 8, 2
      )

      res should be (expected)
    }

    it("should square max with 5 x 5 neighborhood rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, nd,1,   8, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMax(Square(2)).stitch.tile.toArray

      val expected = Array(
        9, 9, 9,    8, 3, 3,    3, 3, 3,
        9, 9, 9,    8, 8, 8,    8, 8, 8,

        9, 9, 9,    8, 8, 8,    8, 8, 8,
        9, 9, 9,    8, 8, 8,    8, 8, 8
      )

      res should be(expected)
    }

    it("should circle max for raster rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, nd,1,   8, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMax(Circle(1)).stitch.tile.toArray

      val expected = Array(
        9, 7, 7,    2, 2, 2,    1, 3, 1,
        9, 9, 2,    3, 3, 3,    3, 3, 3,

        9, 8, 8,    3, 3, 3,    8, 3, 2,
        3, 8, 7,    7, 3, 8,    8, 8, 2
      )

      res should be (expected)
    }

    it("should square max for raster collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, nd,1,   8, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalMax(Square(1)).stitch.tile.toArray

      val expected = Array(
        9, 9, 7,    2, 2, 2,    3, 3, 3,
        9, 9, 8,    3, 3, 3,    3, 3, 3,

        9, 9, 8,    7, 3, 8,    8, 8, 3,
        8, 8, 8,    7, 3, 8,    8, 8, 2
      )

      res should be (expected)
    }

    it("should square max with 5 x 5 neighborhood collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, nd,1,   8, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalMax(Square(2)).stitch.tile.toArray

      val expected = Array(
        9, 9, 9,    8, 3, 3,    3, 3, 3,
        9, 9, 9,    8, 8, 8,    8, 8, 8,

        9, 9, 9,    8, 8, 8,    8, 8, 8,
        9, 9, 9,    8, 8, 8,    8, 8, 8
      )

      res should be(expected)
    }

    it("should circle max for raster collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, nd,1,   8, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalMax(Circle(1)).stitch.tile.toArray

      val expected = Array(
        9, 7, 7,    2, 2, 2,    1, 3, 1,
        9, 9, 2,    3, 3, 3,    3, 3, 3,

        9, 8, 8,    3, 3, 3,    8, 3, 2,
        3, 8, 7,    7, 3, 8,    8, 8, 2
      )

      res should be (expected)
    }

  }
}
