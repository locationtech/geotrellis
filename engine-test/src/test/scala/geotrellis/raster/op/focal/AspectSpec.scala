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

package geotrellis.raster.op.focal

import geotrellis.feature.Extent
import geotrellis.raster._
import geotrellis.engine._
import geotrellis.raster.op._
import geotrellis.testkit._

import org.scalatest._

import scala.math._

import Angles._

class AspectSpec extends FunSpec with Matchers
                                 with TestEngine 
                                 with TileBuilders {
  describe("Aspect") {
    it("should match gdal computed aspect raster") {
      val rasterExtentElevation = RasterSource(LayerId("test:fs", "elevation")).rasterExtent.get
      val rOp = getRaster("elevation")
      val gdalOp = getRaster("aspect")
      val aspectComputed = Aspect(rOp, rasterExtentElevation.cellSize)

      val rasterExtent = RasterSource("aspect").rasterExtent.get
      val rg = get(gdalOp)
      val re = get(aspectComputed)

      // Gdal actually computes the parimeter values differently.
      // So take out the edge results
      val (xmin, ymax) = rasterExtent.gridToMap(1, 1)
      val (xmax, ymin) = rasterExtent.gridToMap(rg.cols - 2, rg.rows - 2)

      val cropExtent = Extent(xmin, ymin, xmax, ymax)

      val rgc = get(gdalOp).convert(TypeDouble).crop(rasterExtent.extent, cropExtent)
      val rc = get(aspectComputed).crop(rasterExtentElevation.extent, cropExtent)

      assertEqual(rgc, rc, 0.1)
    }
  }
}
