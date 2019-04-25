/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.tiling

import geotrellis.proj4.LatLng
import geotrellis.raster.CellSize
import geotrellis.vector.Extent
import org.scalatest._

class FloatingLayoutSchemeSpec extends FunSpec with Matchers {
  describe("FloatingLayoutScheme"){
    val scheme: LayoutScheme = FloatingLayoutScheme(10)
    val level = scheme.levelFor(Extent(0,0,37,27), CellSize(1,1))

    it("should pad the layout to match source resolution"){
      assert(level.layout.tileLayout.totalCols === 40)
      assert(level.layout.tileLayout.totalRows === 30)
    }

    it("should expand the extent to cover padded pixels"){
      assert(level.layout.extent === Extent(0,-3,40,27))
    }

    it("should have enough tiles to cover the source extent"){
      assert(level.layout.layoutCols ===  4)
      assert(level.layout.layoutRows ===  3)
    }
  }
}
