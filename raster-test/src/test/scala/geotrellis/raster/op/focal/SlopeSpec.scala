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

import geotrellis.engine._
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.testkit._
import org.scalatest._

class SlopeSpec extends FunSpec with Matchers
                                with TestEngine {
  describe("Slope") {
    it("should match gdal computed slope raster") {
      val rasterExtent = RasterSource(LayerId("test:fs", "elevation")).rasterExtent.get
      val r = get(getRaster("elevation"))
      val rg = get(getRaster("slope"))
      val slopeComputed = r.slope(rasterExtent.cellSize, 1.0)


      val rasterExtentGdal = RasterSource("slope").rasterExtent.get

      // Gdal actually computes the parimeter values differently.
      // So take out the edge results, but don't throw the baby out
      // with the bathwater. The inner results should match.
      val (xmin, ymax) = rasterExtentGdal.gridToMap(1, 1)
      val (xmax, ymin) = rasterExtentGdal.gridToMap(rg.cols - 2, rg.rows - 2)

      val cropExtent = Extent(xmin, ymin, xmax, ymax)
      val croppedGdal = rg.crop(rasterExtentGdal.extent, cropExtent).convert(TypeDouble)
      val croppedComputed = get(slopeComputed).crop(rasterExtent.extent, cropExtent)

      assertEqual(croppedGdal, croppedComputed, 1.0)
    }

    it("should get the same result for split raster") {
      val rasterExtent = RasterSource(LayerId("test:fs", "elevation")).rasterExtent.get
      val rOp = getRaster("elevation")
      val nonTiledSlope = get(rOp).slope(rasterExtent.cellSize, 1.0)


      val tiled = 
        rOp.map { r =>
          val (tcols, trows) = (11, 20)
          val pcols = rasterExtent.cols / tcols
          val prows = rasterExtent.rows / trows
          val tl = TileLayout(tcols, trows, pcols, prows)
          CompositeTile.wrap(r, tl)
        }

      val rs = RasterSource(tiled, rasterExtent.extent)
      run(rs.slope(1)) match {
        case Complete(result, success) =>
//          println(success)
          assertEqual(result, nonTiledSlope)
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
