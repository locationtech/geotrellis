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

package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

class ArgReaderSpec extends FunSpec
                       with TestEngine 
                       with Matchers {
  describe("ArgReader") {
    it("should read from metadata and match a RasterSource") {
      val fromRasterSource = RasterSource("SBN_inc_percap").get
      val fromArgReader = ArgReader.read("raster-test/data/sbn/SBN_inc_percap.json").tile

      assertEqual(fromArgReader, fromRasterSource)
    }

    it("should read from metadata and match a RasterSource with a target RasterExtent") {
      val RasterExtent(Extent(xmin, ymin, xmax, ymax), cw, ch, cols, rows) = 
        RasterSource("SBN_inc_percap").rasterExtent.get
      val qw = (xmax - xmin) / 4
      val qh = (ymax - ymin) / 4
      val target = RasterExtent(Extent(xmin + qw, ymin + qh, xmax - qw, ymax - qh), cols / 3, rows / 3)

      val fromRasterSource = RasterSource("SBN_inc_percap", target).get
      val fromArgReader = ArgReader.read("raster-test/data/sbn/SBN_inc_percap.json", target).tile

      assertEqual(fromArgReader, fromRasterSource)
    }
  }
}
