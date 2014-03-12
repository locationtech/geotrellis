/**************************************************************************
 * Copyright (c) 2014 Digital Globe.
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
import geotrellis.spark.testfiles.AllHundreds
import geotrellis.spark.testfiles.AllTwos

import org.scalatest.FunSpec

class SubtractSpec extends FunSpec with TestEnvironment with SharedSparkContext with RasterRDDMatchers {

  describe("Subtract Operation") {
    val allTwos = AllTwos(inputHome, conf)
    val allHundreds = AllHundreds(inputHome, conf)

    it("should subtract a constant from a raster") { 

      val twos = RasterRDD(allTwos.path, sc)

      val ones = twos - 1

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should subtract from a constant, raster values") { 

      val twos = RasterRDD(allTwos.path, sc)

      val ones = 3 -: twos

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should subtract multiple rasters") { 
      val hundreds = RasterRDD(allHundreds.path, sc)
      val twos = RasterRDD(allTwos.path, sc)
      val res = hundreds - twos - twos

      shouldBe(res, (96, 96, allHundreds.tileCount))
    }
  }
}
