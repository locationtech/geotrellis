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

package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.summary.polygonal.visitors.MinVisitor
import geotrellis.raster.summary.types.MinValue
import geotrellis.raster.testkit._
import geotrellis.vector._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class MinSpec extends AnyFunSpec with Matchers with RasterMatchers with TileBuilders {

  describe("Min") {
    val rs = createRaster(Array.fill(40*40)(1),40,40)
    val tile = rs.tile
    val extent = rs.extent
    val zone = Extent(10,-10,30,10).toPolygon()

    val multibandTile = MultibandTile(tile, tile, tile)
    val multibandRaster = Raster(multibandTile, extent)

    it("computes Minimum for Singleband") {
      val result = rs.polygonalSummary(zone, MinVisitor)
      result should equal(Summary(MinValue(1.0)))
    }

    it("computes Minimum for Multiband") {
      multibandRaster.polygonalSummary(zone, MinVisitor) match {
        case Summary(result) => result.foreach { _ should equal(MinValue(1.0)) }
        case _ => fail("polygonalSummary did not return a result")
      }
    }
  }
}
