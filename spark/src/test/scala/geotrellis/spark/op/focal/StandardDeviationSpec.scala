/*
 * Copyright (c) 2015 Azavea.
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
package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster.op.focal._
import geotrellis.raster._

import org.scalatest.FunSpec

class StandardDeviationSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Standard Deviation Focal Spec") {

    ifCanRunSpark {

      val nd = NODATA
      val arrayTile = ArrayTile(Array(
            nd,7, 1,   1, 1, 1,   1, 1, 1,
            9, 1, 1,   2, 2, 2,   1, 3, 1,

            3, 8, 1,   3, 3, 3,   1, 1, 2,
            2, 1, 7,   1, nd,1,   8, 1, 1
      ), 9, 4)


      it("should calculate standard deviation raster rdd") {

        val rasterRDD = createRasterRDD(
          sc,
          arrayTile,
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalStandardDeviation(Square(1)).stitch.toArray
        val expected = arrayTile.focalStandardDeviation(Square(1)).toArray
        res should be(expected)
      }

      it("should calculate standard deviation with 5 x 5 neighborhood") {
        val rasterRDD = createRasterRDD(
          sc,
          arrayTile,
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalStandardDeviation(Square(2)).stitch.toArray
        val expected = arrayTile.focalStandardDeviation(Square(2)).toArray
        res should be(expected)
      }

      it("should calculate standard deviation in circle for raster rdd") {
        val rasterRDD = createRasterRDD(
          sc,
          arrayTile,
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalStandardDeviation(Circle(1)).stitch.toArray
        val expected = arrayTile.focalStandardDeviation(Circle(1)).toArray
        res should be (expected)
      }

    }

  }
}
