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

package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.io.LoadFile

import org.scalatest._

import geotrellis.testkit._

class GetStandardDeviationSpec extends FunSpec 
                                  with TestServer
                                  with Matchers {
  describe("GetStandardDeviation") {
    it("should match known values from quad8 raster") {
      val r = get(LoadFile("core-test/data/quad8.arg"))
      val std = get(GetStandardDeviation(r, GetHistogram(r), 1000))

      val d = std.toArray
  
      d(0) should be (-1341)
      d(10) should be (-447)
      d(200) should be (447)
      d(210) should be (1341)
    }
  }
}
