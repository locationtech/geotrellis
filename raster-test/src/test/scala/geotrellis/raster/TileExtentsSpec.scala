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

package geotrellis.raster

import geotrellis._
import geotrellis.vector.Extent

import org.scalatest._

class TileExtentsSpec extends FunSpec with Matchers {
  describe("TileExtents") {
    it("should get middle tile extent by tile index for 3x4 tiles.") {
      val tileLayout = TileLayout(3, 4, 2, 2)
      val extent = Extent(0,0,6,8)
      val tileExtents = TileExtents(extent, tileLayout)
      tileExtents(4) should be (Extent(2.0, 4.0, 4.0, 6.0))
    }
  }
}
