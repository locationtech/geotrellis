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

package geotrellis.spark.summary.polygonal

import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.raster.summary.polygonal._
import geotrellis.raster._
import geotrellis.raster.summary.polygonal.visitors.MaxVisitor
import geotrellis.raster.summary.types.MaxValue
import geotrellis.vector._

import org.scalatest.funspec.AnyFunSpec

class MaxSpec extends AnyFunSpec with TestEnvironment with TestFiles {

  describe("Max Double Zonal Summary Operation") {
    val inc = IncreasingTestFile
    val multi = inc.withContext { _.mapValues { tile => MultibandTile(tile, tile) } }

    val tileLayout = inc.metadata.tileLayout
    val count = (inc.count * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = inc.metadata.extent

    val xd = totalExtent.xmax - totalExtent.xmin
    val yd = totalExtent.ymax - totalExtent.ymin

    val quarterExtent = Extent(
      totalExtent.xmin,
      totalExtent.ymin,
      totalExtent.xmin + xd / 2,
      totalExtent.ymin + yd / 2
    )

    val mp = {
      val xd2 = xd / 4
      val yd2 = yd / 4

      val tri1 = Polygon(
        (totalExtent.xmin + (xd2 / 2), totalExtent.ymax - (yd2 / 2)),
        (totalExtent.xmin + (xd2 / 2) + xd2, totalExtent.ymax - (yd2 / 2)),
        (totalExtent.xmin + (xd2 / 2) + xd2, totalExtent.ymax - (yd2)),
        (totalExtent.xmin + (xd2 / 2), totalExtent.ymax - (yd2 / 2))
      )

      val tri2 = Polygon(
        (totalExtent.xmax - (xd2 / 2), totalExtent.ymin + (yd2 / 2)),
        (totalExtent.xmax - (xd2 / 2) - xd2, totalExtent.ymin + (yd2 / 2)),
        (totalExtent.xmax - (xd2 / 2) - xd2, totalExtent.ymin + (yd2)),
        (totalExtent.xmax - (xd2 / 2), totalExtent.ymin + (yd2 / 2))
      )

      MultiPolygon(tri1, tri2)
    }

    it("should get correct double max over whole raster extent") {
      inc.polygonalSummaryValue(totalExtent.toPolygon, MaxVisitor).toOption.get should be(MaxValue(count - 1))
    }

    it("should get correct double max over whole raster extent for MultibandTileRDD") {
      multi.polygonalSummaryValue(totalExtent.toPolygon, MaxVisitor).toOption.get map { _ should be(MaxValue(count - 1)) }
    }

    it("should get correct double max over a quarter of the extent") {
      val result = inc.polygonalSummaryValue(quarterExtent.toPolygon, MaxVisitor).toOption.get
      val expected = inc.stitch.polygonalSummary(quarterExtent.toPolygon, MaxVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double max over a quarter of the extent for MultibandTileRDD") {
      val result = multi.polygonalSummaryValue(quarterExtent.toPolygon, MaxVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(quarterExtent.toPolygon, MaxVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }

    it("should get correct double max over a two triangle multipolygon") {
      val result = inc.polygonalSummaryValue(mp, MaxVisitor).toOption.get
      val expected = inc.stitch.polygonalSummary(mp, MaxVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double max over a two triangle multipolygon for MultibandTileRDD") {
      val result = multi.polygonalSummaryValue(mp, MaxVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(mp, MaxVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }
  }

  describe("Max Double Zonal Summary Operation (collections api)") {
    val inc = IncreasingTestFile
    val multi = IncreasingTestFile.withContext {
      _.mapValues { tile => MultibandTile(tile, tile) }
    }

    val tileLayout = inc.metadata.tileLayout
    val count = inc.toCollection.length * tileLayout.tileCols * tileLayout.tileRows
    val totalExtent = inc.metadata.extent

    it("should get correct double max over whole raster extent") {
      inc.polygonalSummaryValue(totalExtent.toPolygon, MaxVisitor).toOption.get should be(MaxValue(count - 1))
    }

    it("should get correct double max over whole raster extent for MultibandTiles") {
      multi.polygonalSummaryValue(totalExtent.toPolygon, MaxVisitor).toOption.get map { _ should be(MaxValue(count - 1)) }
    }

    it("should get correct double max over a quarter of the extent") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )

      val result = inc.polygonalSummaryValue(quarterExtent.toPolygon, MaxVisitor).toOption.get
      val expected = inc.stitch.polygonalSummary(quarterExtent.toPolygon, MaxVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double max over a quarter of the extent for MultibandTiles") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )

      val result = multi.polygonalSummaryValue(quarterExtent.toPolygon, MaxVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(quarterExtent.toPolygon, MaxVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }

    it("should get correct double max over a two triangle multipolygon") {
      val xd = totalExtent.xmax - totalExtent.xmin / 4
      val yd = totalExtent.ymax - totalExtent.ymin / 4

      val tri1 = Polygon(
        (totalExtent.xmin + (xd / 2), totalExtent.ymax - (yd / 2)),
        (totalExtent.xmin + (xd / 2) + xd, totalExtent.ymax - (yd / 2)),
        (totalExtent.xmin + (xd / 2) + xd, totalExtent.ymax - (yd)),
        (totalExtent.xmin + (xd / 2), totalExtent.ymax - (yd / 2))
      )

      val tri2 = Polygon(
        (totalExtent.xmax - (xd / 2), totalExtent.ymin + (yd / 2)),
        (totalExtent.xmax - (xd / 2) - xd, totalExtent.ymin + (yd / 2)),
        (totalExtent.xmax - (xd / 2) - xd, totalExtent.ymin + (yd)),
        (totalExtent.xmax - (xd / 2), totalExtent.ymin + (yd / 2))
      )

      val mp = MultiPolygon(tri1, tri2)

      val result = inc.polygonalSummaryValue(mp, MaxVisitor).toOption.get
      val expected = inc.stitch.polygonalSummary(mp, MaxVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double max over a two triangle multipolygon for MultibandTiles") {
      val xd = totalExtent.xmax - totalExtent.xmin / 4
      val yd = totalExtent.ymax - totalExtent.ymin / 4

      val tri1 = Polygon(
        (totalExtent.xmin + (xd / 2), totalExtent.ymax - (yd / 2)),
        (totalExtent.xmin + (xd / 2) + xd, totalExtent.ymax - (yd / 2)),
        (totalExtent.xmin + (xd / 2) + xd, totalExtent.ymax - (yd)),
        (totalExtent.xmin + (xd / 2), totalExtent.ymax - (yd / 2))
      )

      val tri2 = Polygon(
        (totalExtent.xmax - (xd / 2), totalExtent.ymin + (yd / 2)),
        (totalExtent.xmax - (xd / 2) - xd, totalExtent.ymin + (yd / 2)),
        (totalExtent.xmax - (xd / 2) - xd, totalExtent.ymin + (yd)),
        (totalExtent.xmax - (xd / 2), totalExtent.ymin + (yd / 2))
      )

      val mp = MultiPolygon(tri1, tri2)

      val result = multi.polygonalSummaryValue(mp, MaxVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(mp, MaxVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }
  }
}
