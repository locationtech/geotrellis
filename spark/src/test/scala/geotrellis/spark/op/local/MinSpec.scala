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
import geotrellis.spark.RasterRDD
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class MinSpec extends FunSpec
    with TestEnvironment
    with TestFiles    
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Min Operation") {
    ifCanRunSpark { 
      val increasing = IncreasingTestFile
      val decreasing = DecreasingTestFile
      val allHundreds = AllHundredsTestFile

      val cols = increasing.metaData.tileLayout.totalCols
      val rows = increasing.metaData.tileLayout.totalRows

      val tots = cols * rows;

      it("should min a raster with an integer") {
        val inc = increasing
        val thresh = tots / 2
        val res = inc.localMin(thresh)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.min(y * cols + x, thresh)
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should min a raster with a double") {
        val inc = increasing
        val thresh = tots / 2.0
        val res = inc.localMin(thresh)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.min(y * cols + x, thresh)
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should min two rasters") {
        val inc = increasing
        val dec = decreasing
        val res = inc.localMin(dec)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => {
            val decV = cols * rows - (y * cols + x) - 1
            val incV = y * cols + x

            math.min(decV, incV)
          }
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should min three rasters as a seq") {
        val inc = increasing
        val dec = decreasing
        val hundreds = allHundreds
        val res = inc.localMin(Seq(dec, hundreds))

        rasterShouldBe(
          res,
          (x: Int, y: Int) => {
            val decV = cols * rows - (y * cols + x) - 1
            val incV = y * cols + x

            math.min(math.min(decV, incV), 100)
          }
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }
    }
  }
}
