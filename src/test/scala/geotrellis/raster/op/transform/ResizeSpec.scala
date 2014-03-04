/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.raster.op.transform

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class ResizeSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer {
  describe("ResizeRaster") {
    it("should resize quad8 correctly") {

      // double number of rows and cols
      val re = RasterExtent(Extent(-9.5,3.8,150.5,163.8),4.0,4.0,40,40)
      val r = io.LoadFile("src/test/resources/quad8.arg")
      val resize1 = get(Resize(io.LoadFile("src/test/resources/quad8.arg"), re))
      
      val resize2 = get(Resize(io.LoadFile("src/test/resources/quad8.arg"), 40, 40))
      
      List(resize1, resize2).foreach { r =>
        r.cols should be (40)
        r.rows should be (40)

      	r.get(0,0) should be (1)
      	r.get(21,0) should be (2)
      	r.get(0,21) should be (3)
      	r.get(21,21) should be (4)
      }
    }
  }
    it("should resize quad8 to 4x4 correctly") {
      val raster = get(Resize(io.LoadFile("src/test/resources/quad8.arg"), 4, 4))

      raster.cols should be (4)
      raster.rows should be (4)

      val d = raster.toArray

      d(0) should be (1)
      d(3) should be (2)
      d(8) should be (3)
      d(11) should be (4)
    }
  

}
