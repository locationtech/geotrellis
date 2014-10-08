/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class MaxSpec extends FunSpec
    with TestEnvironment
    with SharedSparkContext
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Max Operation") {
    ifCanRunSpark {
      val increasing = IncreasingTestFile(inputHome, conf)
      val decreasing = DecreasingTestFile(inputHome, conf)
      val allHundreds = AllHundredsTestFile(inputHome, conf)

      val cols = increasing.metaData.cols
      val rows = increasing.metaData.rows

      val tots = cols * rows;

      it("should max a raster with an integer") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val thresh = tots / 2
        val res = inc.localMax(thresh)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.max(y * cols + x, thresh)
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should max a raster with a double") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val thresh = tots / 2.0
        val res = inc.localMax(thresh)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.max(y * cols + x, thresh)
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should max two rasters") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val dec = sc.hadoopRasterRDD(decreasing.path)
        val res = inc.localMax(dec)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => {
            val decV = cols * rows - (y * cols + x) - 1
            val incV = y * cols + x

            math.max(decV, incV)
          }
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should max three rasters as a seq") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val dec = sc.hadoopRasterRDD(decreasing.path)
        val hundreds = sc.hadoopRasterRDD(allHundreds.path)
        val res = inc.localMax(Seq(dec, hundreds))

        rasterShouldBe(
          res,
          (x: Int, y: Int) => {
            val decV = cols * rows - (y * cols + x) - 1
            val incV = y * cols + x

            math.max(math.max(decV, incV), 100)
          }
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }
    }
  }
}
