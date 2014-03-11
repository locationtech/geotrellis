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

class LoadRasterLayerInfoSpec extends FunSpec 
                                 with ShouldMatchers 
                                 with TestServer {
  describe("LoadRasterLayerInfo") {
    it("loads a cached raster.") {
      val info = get(LoadRasterLayerInfo("mtsthelens_tiled_cached"))
      info.cached should be (true)
    }

    it("loads a raster with a data store.") {
      val info = get(LoadRasterLayerInfo("test:fs","quadborder"))
      val info2 = get(LoadRasterLayerInfo("test:fs2","quadborder"))

      info.rasterExtent should not be (info2.rasterExtent)
    }
  }
}
