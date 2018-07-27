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

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._

import geotrellis.spark._
import geotrellis.spark.mapalgebra.focal._
import geotrellis.spark.testkit._

import org.scalatest._

class SumSpec extends FunSpec with TestEnvironment {

  describe("Sum Focal Spec") {

    val nd = NODATA

    it("should square sum r = 1 for raster rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,   1, 1, 1,

          1, 1, 1,   3, 3, 3,   1, 1, 1,
          1, 1, 1,   1,nd, 1,   1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Square(1)).stitch.tile.toArray

      val expected = Array(
        3, 5, 7,    8, 9, 8,    7, 6, 4,
        5, 8,12,   15,18,15,   12, 9, 6,

        6, 9,12,   14,17,14,   12, 9, 6,
        4, 6, 8,    9,11, 9,    8, 6, 4
      )

      res should be (expected)
    }

    it("should square sum with 5x5 neighborhood rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,    1, 1, 1,

          1, 1, 1,   3, 3, 3,    1, 1, 1,
          1, 1, 1,   1,nd, 1,    1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Square(2)).stitch.tile.toArray

      val expected = Array(
        8, 14, 20,   24,24,24,    21,15, 9,
        11, 18, 24,  28,28,28,   25,19,12,

        11, 18, 24,  28,28,28,    25,19,12,
        9, 15, 20,   23,23,23,    20,15, 9
      )

      res should be (expected)
    }

    it("should square sum with 5x5 neighborhood for NoData cells") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,    1, 1, 1,

          1, 1, 1,   3, 3, 3,    1, 1, 1,
          1, 1, 1,   1,nd, 1,    1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Square(2), TargetCell.NoData).stitch.tile.toArray

      val expected = Array(
        8,1, 1,   1, 1, 1,   1, 1, 1,
        1, 1, 1,   2, 2, 2,    1, 1, 1,

        1, 1, 1,   3, 3, 3,    1, 1, 1,
        1, 1, 1,   1,23, 1,    1, 1, 1
      )

      res should be (expected)
    }

    it("should circle sum for all cells") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,    1, 1, 1,

          1, 1, 1,   3, 3, 3,    1, 1, 1,
          1, 1, 1,   1,nd, 1,    1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Circle(1)).stitch.tile.toArray

      val expected = Array(
        2, 3, 4,    5, 5, 5,    4, 4, 3,
        3, 5, 6,    9,10, 9,    6, 5, 4,

        4, 5, 7,   10,11,10,    7, 5, 4,
        3, 4, 4,    5, 5, 5,    4, 4, 3
      )

      res should be (expected)
    }

    it("should square sum r = 1 for raster collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,   1, 1, 1,

          1, 1, 1,   3, 3, 3,   1, 1, 1,
          1, 1, 1,   1,nd, 1,   1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalSum(Square(1)).stitch.tile.toArray

      val expected = Array(
        3, 5, 7,    8, 9, 8,    7, 6, 4,
        5, 8,12,   15,18,15,   12, 9, 6,

        6, 9,12,   14,17,14,   12, 9, 6,
        4, 6, 8,    9,11, 9,    8, 6, 4
      )

      res should be (expected)
    }

    it("should square sum with 5x5 neighborhood collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,    1, 1, 1,

          1, 1, 1,   3, 3, 3,    1, 1, 1,
          1, 1, 1,   1,nd, 1,    1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalSum(Square(2)).stitch.tile.toArray

      val expected = Array(
        8, 14, 20,   24,24,24,    21,15, 9,
        11, 18, 24,  28,28,28,   25,19,12,

        11, 18, 24,  28,28,28,    25,19,12,
        9, 15, 20,   23,23,23,    20,15, 9
      )

      res should be (expected)
    }

    it("should circle sum for raster source collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,    1, 1, 1,

          1, 1, 1,   3, 3, 3,    1, 1, 1,
          1, 1, 1,   1,nd, 1,    1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalSum(Circle(1)).stitch.tile.toArray

      val expected = Array(
        2, 3, 4,    5, 5, 5,    4, 4, 3,
        3, 5, 6,    9,10, 9,    6, 5, 4,

        4, 5, 7,   10,11,10,    7, 5, 4,
        3, 4, 4,    5, 5, 5,    4, 4, 3
      )

      res should be (expected)
    }

    it("should copy cells when no one satisfies the focus criteria") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          11,12, 13,   21, 22, 23,   31, 32, 33,
          14, 15, 16,   24, 25, 26,   34, 35, 36,

          41, 42, 43,   51, 52, 53,   61, 62, 63,
          44, 45, 46,   54, 55, 56,   64, 65, 66
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Square(1), TargetCell.NoData).stitch.tile.toArray

      val expected = Array(
        11,12, 13,   21, 22, 23,   31, 32, 33,
        14, 15, 16,   24, 25, 26,   34, 35, 36,

        41, 42, 43,   51, 52, 53,   61, 62, 63,
        44, 45, 46,   54, 55, 56,   64, 65, 66
      )

      res should be (expected)
    }

    it("should square sum r = 1 for na cells") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,   1, 1, 1,

          1, 1, 1,   3, 3, 3,   1, 1, 1,
          1, 1, 1,   1,nd, 1,   1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Square(1), TargetCell.NoData).stitch.tile.toArray

      val expected = Array(
        3,1, 1,   1, 1, 1,   1, 1, 1,
        1, 1, 1,   2, 2, 2,   1, 1, 1,

        1, 1, 1,   3, 3, 3,   1, 1, 1,
        1, 1, 1,   1,11, 1,   1, 1, 1
      )

      res should be (expected)
    }

    it("should square sum r = 1 for data cells") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,   1, 1, 1,

          1, 1, 1,   3, 3, 3,   1, 1, 1,
          1, 1, 1,   1,nd, 1,   1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Square(1), TargetCell.Data).stitch.tile.toArray

      val expected = Array(
        nd, 5, 7,    8, 9, 8,    7, 6, 4,
        5, 8,12,   15,18,15,   12, 9, 6,

        6, 9,12,   14,17,14,   12, 9, 6,
        4, 6, 8,    9,nd, 9,    8, 6, 4
      )

      res should be (expected)
    }

    it("should circle sum for data cells") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,1, 1,   1, 1, 1,   1, 1, 1,
          1, 1, 1,   2, 2, 2,    1, 1, 1,

          1, 1, 1,   3, 3, 3,    1, 1, 1,
          1, 1, 1,   1,nd, 1,    1, 1, 1
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalSum(Circle(1), TargetCell.Data).stitch.tile.toArray

      val expected = Array(
        nd, 3, 4,    5, 5, 5,    4, 4, 3,
        3, 5, 6,    9,10, 9,    6, 5, 4,

        4, 5, 7,   10,11,10,    7, 5, 4,
        3, 4, 4,    5, nd, 5,    4, 4, 3
      )

      res should be (expected)
    }
  }
}
