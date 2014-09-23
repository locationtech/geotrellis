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
import geotrellis.spark.testfiles.AllTwosTestFile

import org.scalatest.FunSpec

class MultiplySpec extends FunSpec
                      with TestEnvironment
                      with SharedSparkContext
                      with RasterRDDMatchers
                      with OnlyIfCanRunSpark {

  describe("Multiply Operation") {
    ifCanRunSpark {
      val allTwos = AllTwosTestFile(inputHome, conf)

      it("should multiply a constant by a raster") {
        val twos = sc.hadoopRasterRDD(allTwos.path)
        val fours = twos * 2

        rasterShouldBe(fours, (4, 4, allTwos.tileCount))
        rastersShouldHaveSameIds(twos, fours)
      }

      it("should multiply multiple rasters") {
        val twos = sc.hadoopRasterRDD(allTwos.path)
        val eights = twos * twos * twos

        rasterShouldBe(eights, (8, 8, allTwos.tileCount))
        rastersShouldHaveSameIds(twos, eights)
      }
    }
  }
}
