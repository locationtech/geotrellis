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

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._

import geotrellis.raster.testkit._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class NotSpec extends AnyFunSpec
                 with Matchers 
                 with RasterMatchers 
                 with TileBuilders {
  describe("Not") {
    it("negates an Int raster") {
      assertEqual(createValueTile(10,9).localNot(),
                  createValueTile(10,-10))
    }
  }
}
