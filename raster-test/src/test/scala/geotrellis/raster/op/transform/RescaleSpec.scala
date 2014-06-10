/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.feature.Extent

import org.scalatest._

import geotrellis.testkit._

class RescaleSpec extends FunSpec 
                     with Matchers 
                     with TestServer {
  describe("RescaleRaster") {
    it("should resize quad8 correctly") {

      // double number of rows and cols
      val re = RasterExtent(Extent(-9.5,3.8,150.5,163.8),4.0,4.0,40,40)
      val raster = get(Rescale(io.LoadFile("core-test/data/quad8.arg"), 2.0))

      raster.cols should be (40)
      raster.rows should be (40)
      raster.rasterExtent should be (re)
      
      val d = raster.toArray
      println(raster.asciiDraw())
      println(raster.rasterExtent)
      raster.get(0,0) should be (1)
      raster.get(21,0) should be (2)
      raster.get(0,21) should be (3)
      raster.get(21,21) should be (4)
    }
  }
}
