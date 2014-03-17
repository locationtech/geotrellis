/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.global

import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class AsArraySpec extends FunSpec with ShouldMatchers 
                                  with TestServer 
                                  with RasterBuilders {
  describe("AsArray") {
    it("should convert int with AsArray") {
      var arr = Array(1,2,3,4,5,6,7)
      val r = createRaster(arr)
      val arr2 = get(AsArray(r))
      arr2 should be (arr)
    }

    it("should convert double with AsArrayDouble") {
      var arr = Array(1.0,2.0,3.0,4.0,5.0,6.0,7.0)
      val r = createRaster(arr)
      val arr2 = get(AsArrayDouble(r))
      arr2 should be (arr)
    }
  }
}
