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
import geotrellis.raster.summary.polygonal.visitors.MaxVisitor
import geotrellis.raster.summary.types.MaxValue
import geotrellis.raster.testkit._
import geotrellis.vector._
import org.scalatest._

class MaxSpec
    extends FunSpec
    with Matchers
    with RasterMatchers
    with TileBuilders {

  describe("Max") {
    val rs = createRaster(Array.fill(40 * 40)(1.0), 40, 40)
    val tile = rs.tile
    val extent = rs.extent
    val zone = Extent(10, -10, 30, 10).toPolygon
    val disjointZone = Extent(50, 50, 60, 60).toPolygon

    val nodataRS = createRaster(Array.fill(40 * 40)(doubleNODATA), 40, 40)
    val nodataTile = nodataRS.tile

    val multibandTile = MultibandTile(tile, tile, tile)
    val multibandRaster = Raster(multibandTile, extent)
    val multibandNoDataTile = MultibandTile(nodataTile, nodataTile, nodataTile)
    val multibandNoDataRaster = Raster(multibandNoDataTile, extent)

    val xd = extent.xmax - extent.xmin / 4
    val yd = extent.ymax - extent.ymin / 4

    val tri1 = Polygon(
      (extent.xmin + (xd / 2), extent.ymax - (yd / 2)),
      (extent.xmin + (xd / 2) + xd, extent.ymax - (yd / 2)),
      (extent.xmin + (xd / 2) + xd, extent.ymax - (yd)),
      (extent.xmin + (xd / 2), extent.ymax - (yd / 2))
    )

    val tri2 = Polygon(
      (extent.xmax - (xd / 2), extent.ymin + (yd / 2)),
      (extent.xmax - (xd / 2) - xd, extent.ymin + (yd / 2)),
      (extent.xmax - (xd / 2) - xd, extent.ymin + (yd)),
      (extent.xmax - (xd / 2), extent.ymin + (yd / 2))
    )

    val mp = MultiPolygon(tri1, tri2)

    it("converts PolygonalSummaryResult ADT to Either") {
      val result = rs.polygonalSummary(zone, MaxVisitor)
      result.toEither should equal(Right(MaxValue(1)))

      val noResult = rs.polygonalSummary(disjointZone, MaxVisitor)
      noResult.toEither should equal(Left(NoIntersection))
    }

    it("converts PolygonalSummaryResult ADT to Option") {
      val result = rs.polygonalSummary(zone, MaxVisitor)
      result.toOption should equal(Some(MaxValue(1.0)))

      val noResult = rs.polygonalSummary(disjointZone, MaxVisitor)
      noResult.toOption should equal(None)
    }

    it("computes Maximum for Singleband") {
      val result = rs.polygonalSummary(zone, MaxVisitor)
      result should equal(Summary(MaxValue(1.0)))
    }

    it("computes NoIntersection for disjoint Singleband polygon") {
      val result = rs.polygonalSummary(disjointZone, MaxVisitor)

      result should equal(NoIntersection)
    }

    it("computes None for Singleband NODATA") {
      val result = nodataRS.polygonalSummary(zone, MaxVisitor)
      val value = result.toOption.get
      value.toOption should equal(None)
    }

    it("computes Maximum for Multiband") {
      val result = multibandRaster
        .polygonalSummary(zone, MaxVisitor)

      result match {
        case Summary(results) => results.foreach { _ should equal(MaxValue(1.0)) }
        case _ => fail("polygonalSummary did not return a result")
      }
    }

    it("computes None for Multiband NODATA") {
      val result = multibandNoDataRaster
        .polygonalSummary(zone, MaxVisitor)

      result match {
        case Summary(results) => results.foreach { _.toOption should equal(None) }
        case _ => fail("polygonalSummary did not return a result")
      }
    }
  }
}
