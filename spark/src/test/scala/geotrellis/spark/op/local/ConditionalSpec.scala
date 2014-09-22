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
import geotrellis.spark.testfiles.{IncreasingFromZero}

import org.scalatest.FunSpec

class ConditionalSpec extends FunSpec
    with TestEnvironment
    with SharedSparkContext
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Conditional If Operation") {
    ifCanRunSpark {
      val increasingTiff = IncreasingFromZero(inputHome, conf)
      val decreasingTiff = DecreasingFromZero(inputHome, conf)

      it("should change values mod 2 to 1, testing integer method") {
        val increasingFromZero = sc.hadoopRasterRDD(increasingTiff.path)
        val res = increasingFromZero.localIf((a: Int) => a % 2 == 0, 1)

        shouldBe(res, (x: Int, y: Int) => if (x * y % 2 == 0) 1 else x * y)
      }

      it("should change values mod 2 to 1 else 0, testing integer method") {
        val increasingFromZero = sc.hadoopRasterRDD(increasingTiff.path)
        val res = increasingFromZero.localIf((a: Int) => a % 2 == 0, 1, 0)

        shouldBe(res, (x: Int, y: Int) => if (x * y % 2 == 0) 1 else 0)
      }

      it("should change values mod 2 to 1, testing double method") {
        val increasingFromZero = sc.hadoopRasterRDD(increasingTiff.path)
        val res = increasingFromZero.localIf((a: Double) => a % 2 == 0, 1)

        shouldBe(res, (x: Int, y: Int) => if (x * y % 2 == 0) 1 else x * y)
      }

      it("should change values mod 2 to 1 else 0, testing double method") {
        val increasingFromZero = sc.hadoopRasterRDD(increasingTiff.path)
        val res = increasingFromZero.localIf((a: Double) => a % 2 == 0, 1, 0)

        shouldBe(res, (x: Int, y: Int) => if (x * y % 2 == 0) 1 else 0)
      }
    }
  }
}
