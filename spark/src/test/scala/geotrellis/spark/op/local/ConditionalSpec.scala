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
import geotrellis.spark.testfiles.{IncreasingFromZero, DecreasingFromZero}

import org.scalatest.FunSpec

class ConditionalSpec extends FunSpec
    with TestEnvironment
    with SharedSparkContext
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Conditional If Operation") {
    ifCanRunSpark {
      val increasing = IncreasingFromZero(inputHome, conf)
      val decreasing = DecreasingFromZero(inputHome, conf)

      val cols = increasing.metaData.cols
      val rows = increasing.metaData.rows
      val tots = cols * rows - 1

      it("should change values mod 2 to 1, testing integer method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = inc.localIf((a: Int) => a % 2 == 0, 1)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else y * cols + x
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should change values mod 2 to 1 else 0, testing integer method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = inc.localIf((a: Int) => a % 2 == 0, 1, 0)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else 0
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should change values mod 2 to 1, testing double method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = inc.localIf((a: Double) => a % 2 == 0, 1)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else y * cols + x
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should change values mod 2 to 1 else 0, testing double method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val res = inc.localIf((a: Double) => a % 2 == 0, 1, 0)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else 0
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 integer method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val dec = sc.hadoopRasterRDD(decreasing.path)

        val thres = tots / 2.0

        val res = inc.localIf(dec, (a: Int, b: Int) => math.abs(a - b) <= 1, 0)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else y * cols + x
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 else 1 integer method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val dec = sc.hadoopRasterRDD(decreasing.path)

        val thres = tots / 2.0

        val res = inc.localIf(dec, (a: Int, b: Int) => math.abs(a - b) <= 1, 0, 0)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else 0
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 double method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val dec = sc.hadoopRasterRDD(decreasing.path)

        val thres = tots / 2.0

        val res = inc.localIf(dec, (a: Double, b: Double) => math.abs(a - b) <= 1, 0)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else y * cols + x
        )

        rastersShouldHaveSameIds(res, inc)
      }

      it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 else 1 double method") {
        val inc = sc.hadoopRasterRDD(increasing.path)
        val dec = sc.hadoopRasterRDD(decreasing.path)

        val thres = tots / 2.0

        val res = inc.localIf(dec, (a: Double, b: Double) => math.abs(a - b) <= 1, 0, 0)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else 0
        )

        rastersShouldHaveSameIds(res, inc)
      }
    }
  }
}
