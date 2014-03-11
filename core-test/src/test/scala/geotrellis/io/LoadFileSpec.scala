/***
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
 ***/

package geotrellis.io

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class LoadFileSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer 
                      with RasterBuilders {
  describe("LoadFile") {
    it("loads a test raster.") {
      val raster = get(LoadFile("core-test/data/fake.img8.arg"))

      raster.get(0, 0) should be (49)
      raster.get(3, 3) should be (4)
    }

    it("should load fake.img8 with resampling") {
      val realRaster = get(io.LoadFile("core-test/data/fake.img8.arg"))
      val re = get(io.LoadRasterExtentFromFile("core-test/data/fake.img8.arg"))
      val extent = re.extent

      val resampleRasterExtent = RasterExtent(extent, 2, 2) 
      val raster = get(io.LoadFile("core-test/data/fake.img8.arg", resampleRasterExtent))
      printR(realRaster)
      println(re)
      printR(raster)
      println(resampleRasterExtent)

      raster.get(0, 0) should be (34)
      raster.get(1, 0) should be (36)
      raster.get(0, 1) should be (2)
      raster.get(1, 1) should be (4)
    }
  }
}
