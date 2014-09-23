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
import geotrellis.spark.testfiles.{IncreasingTestFile, AllOnesTestFile}

import org.scalatest.FunSpec

class LessSpec extends FunSpec
    with TestEnvironment
    with SharedSparkContext
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Less Operation") {
    ifCanRunSpark {
      val increasing = IncreasingTestFile(inputHome, conf)
      val allOnes = AllOnesTestFile(inputHome, conf)

      val cols = increasing.metaData.cols

      it("should check less between an integer and a raster") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = inc < 1

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 0 && y == 0) 1 else 0
        )

        rastersShouldHaveSameIds(inc, res)
      }

      it("should check less right associative between an integer and a raster") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = 1 <<: inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x <= 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIds(inc, res)
      }

      it("should check less between a double and a raster") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = inc < 1.0

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 0 && y == 0) 1 else 0
        )

        rastersShouldHaveSameIds(inc, res)
      }

      it("should check less right associative between a double and a raster") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = 1.0 <<: inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x <= 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIds(inc, res)
      }

      it("should check less between two rasters") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val ones = sc.hadoopRasterRDD(allOnes.path)
        val res = inc < ones

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 0 && y == 0) 1 else 0
        )

        rastersShouldHaveSameIds(inc, res)
      }
    }
  }
}
