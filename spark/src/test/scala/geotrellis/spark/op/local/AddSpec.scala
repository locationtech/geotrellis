/**************************************************************************
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
 **************************************************************************/

package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.testfiles.AllOnes

import org.scalatest.FunSpec

class AddSpec extends FunSpec with TestEnvironment with SharedSparkContext with RasterRDDMatchers {

  describe("Add Operation") {
    val allOnes = AllOnes(inputHome, conf)

    it("should add a constant to a raster") { 

      val ones = RasterRDD(allOnes.path, sc)
      
      val twos = ones + 1

      shouldBe(twos, (2, 2, allOnes.tileCount))
    }

    it("should add multiple rasters") {

      val ones = RasterRDD(allOnes.path, sc)

      val threes = ones + ones + ones 

      shouldBe(threes, (3, 3, allOnes.tileCount))
    }
  }
}
