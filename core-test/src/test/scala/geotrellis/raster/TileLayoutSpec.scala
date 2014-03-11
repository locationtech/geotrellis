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

package geotrellis.raster

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class TileLayoutSpec extends FunSpec with ShouldMatchers {
  describe("TileLayout.fromTileDimensions") {
    it("should map an inverse function correctly.") {
      val extent = Extent(0,0,1000,1000)
      val re = RasterExtent(extent,10, 1, 100, 1000)
      val TileLayout(tileCols,tileRows,pixelCols,pixelRows) = TileLayout.fromTileDimensions(re,40,600)
      tileCols should be (3)
      tileRows should be (2)
      pixelCols should be (40)
      pixelRows should be (600)
    }
  }
}
