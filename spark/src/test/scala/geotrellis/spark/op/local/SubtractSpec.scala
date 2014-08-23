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
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class SubtractSpec extends FunSpec 
                      with TestEnvironment 
                      with SharedSparkContext 
                      with RasterRDDMatchers
                      with OnlyIfCanRunSpark {

  describe("Subtract Operation") {
    ifCanRunSpark {
      val allOnes = AllOnes(inputHome, conf)
      val allTwos = AllTwos(inputHome, conf)
      val allHundreds = AllHundreds(inputHome, conf)

      it("should subtract a constant from a raster") {

        val twos = sc.hadoopRasterRDD(allTwos.path)

        val ones = twos - 1

        shouldBe(ones, (1, 1, allTwos.tileCount))
      }

      it("should subtract from a constant, raster values") {

        val twos = sc.hadoopRasterRDD(allTwos.path)

        val ones = 3 -: twos

        shouldBe(ones, (1, 1, allTwos.tileCount))
      }

      it("should subtract multiple rasters") {
        val hundreds = sc.hadoopRasterRDD(allHundreds.path)
        val ones = sc.hadoopRasterRDD(allOnes.path)
        val twos = sc.hadoopRasterRDD(allTwos.path)
        val res = hundreds - twos - ones

        shouldBe(res, (97, 97, allHundreds.tileCount))
      }
    }
  }
}
