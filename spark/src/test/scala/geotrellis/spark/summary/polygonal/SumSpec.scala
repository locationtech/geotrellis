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
import geotrellis.raster.summary.polygonal.visitors.SumVisitor
import geotrellis.raster.summary.types.SumValue
import geotrellis.vector._

import org.scalatest.funspec.AnyFunSpec

class SumSpec extends AnyFunSpec with TestEnvironment with TestFiles {

  describe("Sum Double Zonal Summary Operation") {
    val ones = AllOnesTestFile
    val multi = ones.withContext { _.mapValues { tile => MultibandTile(tile, tile, tile) }}

    val tileLayout = ones.metadata.tileLayout
    val count = (ones.count * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = ones.metadata.extent

    val xd = totalExtent.xmax - totalExtent.xmin
    val yd = totalExtent.ymax - totalExtent.ymin

    val quarterExtent = Extent(
      totalExtent.xmin,
      totalExtent.ymin,
      totalExtent.xmin + xd / 2,
      totalExtent.ymin + yd / 2
    )

    val p1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax)
    val p2 = Point(totalExtent.xmax, totalExtent.ymin + yd / 2)
    val p3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin)
    val p4 = Point(totalExtent.xmin, totalExtent.ymin + yd / 2)

    val diamondPoly = Polygon(LineString(Array(p1, p2, p3, p4, p1)))

    val polyWithHole = {
      val exterior = LineString(Array(p1, p2, p3, p4, p1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = LineString(Array(pi1, pi2, pi3, pi4, pi1))

      Polygon(exterior, interior)
    }

    it("should get correct double sum over whole raster extent") {
      ones.polygonalSummaryValue(totalExtent.toPolygon, SumVisitor).toOption.get should be(SumValue(count))
    }

    it("should get correct double sum over whole raster extent for MultibandTileRDD") {
      multi.polygonalSummaryValue(totalExtent.toPolygon, SumVisitor).toOption.get map { _  should be(SumValue(count)) }
    }

    it("should get correct double sum over a quarter of the extent") {
      val result = ones.polygonalSummaryValue(quarterExtent.toPolygon, SumVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(quarterExtent.toPolygon, SumVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double sum over a quarter of the extent for MultibandTileRDD") {
      val result = multi.polygonalSummaryValue(quarterExtent.toPolygon, SumVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(quarterExtent.toPolygon, SumVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }

    it("should get correct double sum over half of the extent in diamond shape") {
      val result = ones.polygonalSummaryValue(diamondPoly, SumVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(diamondPoly, SumVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double sum over half of the extent in diamond shape for MultibandTileRDD") {
      val result = multi.polygonalSummaryValue(diamondPoly, SumVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(diamondPoly, SumVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }

    it("should get correct double sum over polygon with hole") {
      val result = ones.polygonalSummaryValue(polyWithHole, SumVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(polyWithHole, SumVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double sum over polygon with hole for MultibandTileRDD") {
      val result = multi.polygonalSummaryValue(polyWithHole, SumVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(polyWithHole, SumVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }
  }

  describe("Sum Double Zonal Summary Operation (collections api)") {
    val ones = AllOnesTestFile
    val multi = AllOnesTestFile.withContext { _.mapValues { tile => MultibandTile(tile, tile, tile) }}

    val tileLayout = ones.metadata.tileLayout
    val count = ones.toCollection.length * tileLayout.tileCols * tileLayout.tileRows
    val totalExtent = ones.metadata.extent

    val xd = totalExtent.xmax - totalExtent.xmin
    val yd = totalExtent.ymax - totalExtent.ymin

    val quarterExtent = Extent(
      totalExtent.xmin,
      totalExtent.ymin,
      totalExtent.xmin + xd / 2,
      totalExtent.ymin + yd / 2
    )

    val p1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax)
    val p2 = Point(totalExtent.xmax, totalExtent.ymin + yd / 2)
    val p3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin)
    val p4 = Point(totalExtent.xmin, totalExtent.ymin + yd / 2)

    val diamondPoly = Polygon(LineString(Array(p1, p2, p3, p4, p1)))

    val polyWithHole = {
      val exterior = LineString(Array(p1, p2, p3, p4, p1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = LineString(Array(pi1, pi2, pi3, pi4, pi1))

      Polygon(exterior, interior)
    }

    it("should get correct double sum over whole raster extent") {
      ones.polygonalSummaryValue(totalExtent.toPolygon, SumVisitor).toOption.get should be(SumValue(count))
    }

    it("should get correct double sum over whole raster extent for MultibandTiles") {
      multi.polygonalSummaryValue(totalExtent.toPolygon, SumVisitor).toOption.get map { _  should be(SumValue(count)) }
    }

    it("should get correct double sum over a quarter of the extent") {
      val result = ones.polygonalSummaryValue(quarterExtent.toPolygon, SumVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(quarterExtent.toPolygon, SumVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double sum over a quarter of the extent for MultibandTiles") {
      val result = multi.polygonalSummaryValue(quarterExtent.toPolygon, SumVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(quarterExtent.toPolygon, SumVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }

    it("should get correct double sum over half of the extent in diamond shape") {
      val result = ones.polygonalSummaryValue(diamondPoly, SumVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(diamondPoly, SumVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double sum over half of the extent in diamond shape for MultibandTiles") {
      val result = multi.polygonalSummaryValue(diamondPoly, SumVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(diamondPoly, SumVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }

    it("should get correct double sum over polygon with hole") {
      val result = ones.polygonalSummaryValue(polyWithHole, SumVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(polyWithHole, SumVisitor).toOption.get

      result should be (expected)
    }

    it("should get correct double sum over polygon with hole for MultibandTiles") {
      val result = multi.polygonalSummaryValue(polyWithHole, SumVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(polyWithHole, SumVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res should be (exp)
      }
    }
  }
}
