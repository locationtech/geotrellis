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

class MedianSpec extends FunSpec with TestEnvironment {

  describe("Median Focal Spec") {

    val nd = NODATA

    it("should square median for raster rdd") {
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

      val res = rasterRDD.focalMedian(Square(1)).stitch.tile.toArray

      val expected = Array(
        7, 1, 1,    1, 2, 3,    4, 4, 4,
        7, 2, 1,    2, 3, 3,    3, 3, 2,

        3, 3, 2,    2, 2, 2,    3, 3, 3,
        3, 3, 3,    3, 3, 3,    2, 2, 2
      )

      res should be (expected)
    }

    it("should square median for raster collection") {
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

      val res = rasterCollection.focalMedian(Square(1)).stitch.tile.toArray

      val expected = Array(
        7, 1, 1,    1, 2, 3,    4, 4, 4,
        7, 2, 1,    2, 3, 3,    3, 3, 2,

        3, 3, 2,    2, 2, 2,    3, 3, 3,
        3, 3, 3,    3, 3, 3,    2, 2, 2
      )

      res should be (expected)
    }

    it("should square median for data cells") {
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

      val res = rasterRDD.focalMedian(Square(1), TargetCell.Data).stitch.tile.toArray

      val expected = Array(
        nd, 1, 1,    1, 2, 3,    4, 4, 4,
        7, 2, 1,    2, 3, 3,    3, 3, 2,

        3, 3, 2,    2, 2, 2,    3, 3, 3,
        3, 3, 3,    3, nd, 3,    2, 2, 2
      )

      res should be (expected)
    }
  }
}
