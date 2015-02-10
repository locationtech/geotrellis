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

package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.raster.op.stats._

import org.scalatest._

import geotrellis.testkit._

class GetColorBreaksSpec extends FunSpec
                            with TestEngine
                            with Matchers {
  describe("GetColorBreaks") {
    it("gets color breaks for test raster.") {
      val testTile = {
        val nd = NODATA
        ArrayTile(
          Array(
            12, 12, 13, 14, 15,
            44, 91, nd, 11, 95,
            12, 13, 56, 66, 66,
            44, 91, nd, 11, 95),
          5, 4)
      }

      val h = testTile.histogram
      val (g, y, o, r) = (0x00ff00ff, 0xffff00ff, 0xff7f00ff, 0xff0000ff)
      val colors = Array(g, y, o, r)
      val colorBreaks = ColorBreaks(h, colors)
      colorBreaks.limits should be (Array(12, 15, 66, 95))
      colorBreaks.colors should be (Array(g, y, o, r))
    }
  }
}
