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
import geotrellis.raster.op._

import geotrellis.testkit.TestServer
import geotrellis.source.RasterSource
import geotrellis.statistics.Statistics

import org.scalatest._

class StatOpMethodsSpec extends FunSpec with TestServer with Matchers {
  def rasterSource = RasterSource("quad_tiled")

  describe("StatOpMethods") {
    it("should calculate .statistics()") {
      val statSource = rasterSource.statistics()
      val stats = statSource.get

      val dev = math.sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics(2.5, 3, 1, dev, 1, 4)

      stats should be (expected)
    }
  }
}
