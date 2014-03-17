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

package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.io.LoadFile

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class GetClassBreaksSpec extends FunSpec 
                            with TestServer
                            with ShouldMatchers {
  def testRaster = {
    val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)
    val nd = NODATA
    val data1 = Array(12, 12, 13, 14, 15,
                      44, 91, nd, 11, 95,
                      12, 13, 56, 66, 66,
                      44, 91, nd, 11, 95)
    Raster(data1, rasterExtent)
  }

  describe("GetClassBreaks") {
    it("gets expected class breaks from test raster.") {
      val h = GetHistogram(Literal(testRaster))
      val result = GetClassBreaks(h, 4)
      get(result) should be (Array(12, 15, 66, 95))
    }
  }
}
