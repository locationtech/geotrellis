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
import geotrellis.spark.testkit._

import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster._

import org.scalatest.FunSpec

class MeanSpec extends FunSpec with TestEnvironment {

  describe("Mean Focal Spec") {
    val nd = NODATA
    val dnd = doubleNODATA

    it("should square mean for raster rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 3, 5,   9, 8, 2,
          9, 1, 1,   2, 2, 2,   4, 3, 5,

          3, 8, 1,   3, 3, 3,   1, 2, 2,
          2, 4, 7,   1,nd, 1,   8, 4, 3
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMean(Square(1)).stitch.tile.toArrayDouble

      val expected = Array(
        5.666, 3.8, 2.166,   1.666,   2.5, 4.166,    5.166, 5.166,   4.5,
        5.6, 3.875, 2.777,   1.888, 2.666, 3.555,    4.111,   4.0, 3.666,

        4.5,  4.0, 3.111,    2.5, 2.125,   3.0,      3.111, 3.555, 3.166,
        4.25, 4.166, 4.0,    3.0,   2.2,   3.2,      3.166, 3.333,  2.75
      )

      arraysEqual(res, expected)
    }

    it("should square mean only for na") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 3, 5,   9, 8, 2,
          9, 1, 1,   2, 2, 2,   4, 3, 5,

          3, 8, 1,   3, 3, 3,   1, 2, 2,
          2, 4, 7,   1,nd, 1,   8, 4, 3
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMean(Square(1), TargetCell.NoData).stitch.tile.toArrayDouble

      val expected = Array(
        5.666,7, 1,   1, 3, 5,   9, 8, 2,
        9, 1, 1,   2, 2, 2,   4, 3, 5,

        3, 8, 1,   3, 3, 3,   1, 2, 2,
        2, 4, 7,   1, 2.2, 1,   8, 4, 3
      )

      arraysEqual(res, expected)
    }

    it("should square mean only for na for double raster") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          dnd,7.0, 1.0,   1, 3, 5,   9.5, 8.5, 2.5,
          9, 1, 1,   2, 2, 2,   4, 3, 5,

          3, 8, 1,   3, 3, 3,   1, 2, 2,
          2, 4, 7,   1,dnd, 1,   8, 4, 3
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMean(Square(1), TargetCell.NoData).stitch.tile.toArrayDouble

      val expected = Array(
        5.666,7, 1,   1, 3, 5,   9.5, 8.5, 2.5,
        9, 1, 1,   2, 2, 2,   4, 3, 5,

        3, 8, 1,   3, 3, 3,   1, 2, 2,
        2, 4, 7,   1, 2.2, 1,   8, 4, 3
      )

      arraysEqual(res, expected)
    }

    it("should circle mean for raster rdd") {
      val rasterRDD = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          5.666,  3.8, 2.166,  1.666,   2.5, 4.166,  5.166, 5.166,   4.5,
          5.6, 3.875, 2.777,   1.888, 2.666, 3.555,  4.111,   4.0, 3.666,

          4.5,  4.0, 3.111,    2.5, 2.125,   3.0,    3.111, 3.555, 3.166,
          4.25, 4.166, 4.0,    3.0,   2.2,   3.2,    3.166, 3.333,  2.75
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMean(Circle(1)).stitch.tile.toArrayDouble

      val expected = Array(
        5.022,3.876,2.602,    2.054, 2.749, 3.846,    4.652, 4.708, 4.444,
        4.910, 4.01,2.763,    2.299, 2.546, 3.499,    3.988, 4.099, 3.833,

        4.587, 3.93,3.277,    2.524, 2.498, 2.998,    3.388, 3.433, 3.284,
        4.305,4.104,3.569,    2.925, 2.631, 2.891,    3.202, 3.201, 3.083
      )

      arraysEqual(res, expected)
    }

    it("should square mean for raster collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 3, 5,   9, 8, 2,
          9, 1, 1,   2, 2, 2,   4, 3, 5,

          3, 8, 1,   3, 3, 3,   1, 2, 2,
          2, 4, 7,   1,nd, 1,   8, 4, 3
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalMean(Square(1)).stitch.tile.toArrayDouble

      val expected = Array(
        5.666, 3.8, 2.166,   1.666,   2.5, 4.166,    5.166, 5.166,   4.5,
        5.6, 3.875, 2.777,   1.888, 2.666, 3.555,    4.111,   4.0, 3.666,

        4.5,  4.0, 3.111,    2.5, 2.125,   3.0,      3.111, 3.555, 3.166,
        4.25, 4.166, 4.0,    3.0,   2.2,   3.2,      3.166, 3.333,  2.75
      )

      arraysEqual(res, expected)
    }

    it("should circle mean for raster collection") {
      val rasterCollection = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          5.666,  3.8, 2.166,  1.666,   2.5, 4.166,  5.166, 5.166,   4.5,
          5.6, 3.875, 2.777,   1.888, 2.666, 3.555,  4.111,   4.0, 3.666,

          4.5,  4.0, 3.111,    2.5, 2.125,   3.0,    3.111, 3.555, 3.166,
          4.25, 4.166, 4.0,    3.0,   2.2,   3.2,    3.166, 3.333,  2.75
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      ).toCollection

      val res = rasterCollection.focalMean(Circle(1)).stitch.tile.toArrayDouble

      val expected = Array(
        5.022,3.876,2.602,    2.054, 2.749, 3.846,    4.652, 4.708, 4.444,
        4.910, 4.01,2.763,    2.299, 2.546, 3.499,    3.988, 4.099, 3.833,

        4.587, 3.93,3.277,    2.524, 2.498, 2.998,    3.388, 3.433, 3.284,
        4.305,4.104,3.569,    2.925, 2.631, 2.891,    3.202, 3.201, 3.083
      )

      arraysEqual(res, expected)
    }
  }
}
