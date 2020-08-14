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
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.raster._
import geotrellis.raster.summary.polygonal._
import geotrellis.raster.summary.polygonal.visitors._
import geotrellis.vector._
import geotrellis.spark.testkit._

import org.scalatest.funspec.AnyFunSpec

import collection.immutable.HashMap

class HistogramSpec extends AnyFunSpec with TestEnvironment with TestFiles {
  describe("Histogram Zonal Summary Operation") {

    val modHundred = Mod10000TestFile
    val multiModHundred = modHundred.withContext { _.mapValues { MultibandTile(_) }}
    val ones = AllOnesTestFile
    val inc = IncreasingTestFile
    val multi = ones.withContext { _.mapValues { tile => MultibandTile(tile, tile, tile) }}

    val tileLayout = modHundred.metadata.tileLayout
    val count = (modHundred.count * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = modHundred.metadata.extent

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

    it("should get correct histogram over whole raster extent") {
      // We use FastMapHistogram for this test because StreamingHistogram can shuffle bucket
      //  bounds and counts based on the order in which dissimilar elements are added. This addition is
      //  non-deterministic for our RDD polygonal summaries.
      val histogram = modHundred.polygonalSummaryValue(totalExtent.toPolygon, FastMapHistogramVisitor).toOption.get
      val expected = modHundred.stitch.polygonalSummary(totalExtent.toPolygon, FastMapHistogramVisitor).toOption.get

      histogram.totalCount should be (expected.totalCount)
      histogram.foreachValue(v => histogram.itemCount(v) should be (expected.itemCount(v)))

      var map = HashMap[Int, Int]()
      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }
      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    // We use FastMapHistogram for this test because StreamingHistogram can shuffle bucket
    //  bounds and counts based on the order in which dissimilar elements are added. This addition is
    //  non-deterministic for our RDD polygonal summaries.
    it("should get correct histogram over whole raster extent for a MultibandTileRDD") {
      val histogram = multiModHundred.polygonalSummaryValue(
        totalExtent.toPolygon,
        FastMapHistogramVisitor
      ).toOption.get.head
      val expected = multiModHundred.stitch.polygonalSummary(
        totalExtent.toPolygon,
        FastMapHistogramVisitor
      ).toOption.get.head

      histogram.totalCount should be (expected.totalCount)
      histogram.foreachValue(v => histogram.itemCount(v) should be (expected.itemCount(v)))

      var map = HashMap[Int, Int]()
      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }
      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    it("should get correct histogram over a quarter of the extent") {
      val histogram = inc.polygonalSummaryValue(
        quarterExtent.toPolygon,
        StreamingHistogramVisitor).toOption.get
      val expected = inc.stitch.polygonalSummary(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over a quarter of the extent for a MultibandTileRDD") {
      val histogram = multi.polygonalSummaryValue(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }

    it("should get correct histogram over half of the extent in diamond shape") {
      val histogram = ones.polygonalSummaryValue(diamondPoly, StreamingHistogramVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(diamondPoly, StreamingHistogramVisitor).toOption.get

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over half of the extent in diamond shape for a MultibandTileRDD") {
      val histogram = multi.polygonalSummaryValue(diamondPoly, StreamingHistogramVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(diamondPoly, StreamingHistogramVisitor).toOption.get

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }

    it("should get correct histogram over polygon with hole") {
      val histogram = ones.polygonalSummaryValue(polyWithHole, StreamingHistogramVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(polyWithHole, StreamingHistogramVisitor).toOption.get

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over polygon with hole for a MultibandTileRDD") {
      val histogram = multi.polygonalSummaryValue(polyWithHole, StreamingHistogramVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(polyWithHole, StreamingHistogramVisitor).toOption.get

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }
  }

  describe("Histogram Zonal Summary Operation (collections api)") {
    val modHundred = Mod10000TestFile
    val multiModHundred = Mod10000TestFile.withContext { _.mapValues { MultibandTile(_) }}
    val ones = AllOnesTestFile
    val multi = AllOnesTestFile.withContext { _.mapValues { tile => MultibandTile(tile, tile, tile) }}

    val tileLayout = modHundred.metadata.tileLayout
    val count = modHundred.toCollection.length * tileLayout.tileCols * tileLayout.tileRows
    val totalExtent = modHundred.metadata.extent

    it("should get correct histogram over whole raster extent") {
      val histogram = modHundred.polygonalSummaryValue(totalExtent.toPolygon, FastMapHistogramVisitor).toOption.get

      var map = HashMap[Int, Int]()

      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }

      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    it("should get correct histogram over whole raster extent for MultibandTiles") {
      val histogram = multiModHundred.polygonalSummaryValue(totalExtent.toPolygon, FastMapHistogramVisitor).toOption.get.head

      var map = HashMap[Int, Int]()

      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }

      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    it("should get correct histogram over a quarter of the extent") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )

      val histogram = ones.polygonalSummaryValue(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over a quarter of the extent for MultibandTiles") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )

      val histogram = multi.polygonalSummaryValue(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(quarterExtent.toPolygon, StreamingHistogramVisitor).toOption.get

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }

    it("should get correct histogram over half of the extent in diamond shape") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin


      val p1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax)
      val p2 = Point(totalExtent.xmax, totalExtent.ymin + yd / 2)
      val p3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin)
      val p4 = Point(totalExtent.xmin, totalExtent.ymin + yd / 2)

      val poly = Polygon(LineString(Array(p1, p2, p3, p4, p1)))

      val histogram = ones.polygonalSummaryValue(poly, StreamingHistogramVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(poly, StreamingHistogramVisitor).toOption.get

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over half of the extent in diamond shape for MultibandTiles") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val p1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax)
      val p2 = Point(totalExtent.xmax, totalExtent.ymin + yd / 2)
      val p3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin)
      val p4 = Point(totalExtent.xmin, totalExtent.ymin + yd / 2)

      val poly = Polygon(LineString(Array(p1, p2, p3, p4, p1)))

      val histogram = multi.polygonalSummaryValue(poly, StreamingHistogramVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(poly, StreamingHistogramVisitor).toOption.get

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }

    it("should get correct histogram over polygon with hole") {
      val delta = 0.0001 // A bit of a delta to avoid floating point errors.

      val xd = totalExtent.width + delta
      val yd = totalExtent.height + delta


      val pe1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax)
      val pe2 = Point(totalExtent.xmax, totalExtent.ymin + yd / 2)
      val pe3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin)
      val pe4 = Point(totalExtent.xmin, totalExtent.ymin + yd / 2)

      val exterior = LineString(Array(pe1, pe2, pe3, pe4, pe1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = LineString(Array(pi1, pi2, pi3, pi4, pi1))
      val poly = Polygon(exterior, interior)

      val histogram = ones.polygonalSummaryValue(poly, StreamingHistogramVisitor).toOption.get
      val expected = ones.stitch.polygonalSummary(poly, StreamingHistogramVisitor).toOption.get

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over polygon with hole for MultibandTiles") {
      val delta = 0.0001 // A bit of a delta to avoid floating point errors.

      val xd = totalExtent.width + delta
      val yd = totalExtent.height + delta


      val pe1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax)
      val pe2 = Point(totalExtent.xmax, totalExtent.ymin + yd / 2)
      val pe3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin)
      val pe4 = Point(totalExtent.xmin, totalExtent.ymin + yd / 2)

      val exterior = LineString(Array(pe1, pe2, pe3, pe4, pe1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = LineString(Array(pi1, pi2, pi3, pi4, pi1))
      val poly = Polygon(exterior, interior)

      val histogram = multi.polygonalSummaryValue(poly, StreamingHistogramVisitor).toOption.get
      val expected = multi.stitch.polygonalSummary(poly, StreamingHistogramVisitor).toOption.get

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }
  }
}
