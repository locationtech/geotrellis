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
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit.testfiles._
import geotrellis.raster._
import geotrellis.raster.summary.polygonal._
import geotrellis.vector._
import geotrellis.spark.testkit._

import org.apache.spark.SparkContext
import org.scalatest.FunSpec

import collection.immutable.HashMap

class HistogramSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Histogram Zonal Summary Operation") {
    val modHundred = Mod10000TestFile
    val multiModHundred = modHundred.withContext { _.mapValues { MultibandTile(_) }}
    val ones = AllOnesTestFile
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

    val diamondPoly = Polygon(Line(Array(p1, p2, p3, p4, p1)))

    val polyWithHole = {
      val exterior = Line(Array(p1, p2, p3, p4, p1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = Line(Array(pi1, pi2, pi3, pi4, pi1))

      Polygon(exterior, interior)
    }

    it("should get correct histogram over whole raster extent") {
      val histogram = modHundred.polygonalHistogram(totalExtent.toPolygon)

      var map = HashMap[Int, Int]()

      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }

      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    it("should get correct histogram over whole raster extent for a MultibandTileRDD") {
      val histogram = multiModHundred.polygonalHistogram(totalExtent.toPolygon).head

      var map = HashMap[Int, Int]()

      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }

      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    it("should get correct histogram over a quarter of the extent") {
      val histogram = ones.polygonalHistogram(quarterExtent.toPolygon)
      val expected = ones.stitch.tile.polygonalHistogram(totalExtent, quarterExtent.toPolygon)

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over a quarter of the extent for a MultibandTileRDD") {
      val histogram = multi.polygonalHistogram(quarterExtent.toPolygon)
      val expected = multi.stitch.tile.polygonalHistogram(totalExtent, quarterExtent.toPolygon)

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }

    it("should get correct histogram over half of the extent in diamond shape") {
      val histogram = ones.polygonalHistogram(diamondPoly)
      val expected = ones.stitch.tile.polygonalHistogram(totalExtent, diamondPoly)

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over half of the extent in diamond shape for a MultibandTileRDD") {
      val histogram = multi.polygonalHistogram(diamondPoly)
      val expected = multi.stitch.tile.polygonalHistogram(totalExtent, diamondPoly)

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }

    it("should get correct histogram over polygon with hole") {
      val histogram = ones.polygonalHistogram(polyWithHole)
      val expected = ones.stitch.tile.polygonalHistogram(totalExtent, polyWithHole)

      histogram.minMaxValues should be (expected.minMaxValues)
      histogram.itemCount(1) should be (expected.itemCount(1))
    }

    it("should get correct histogram over polygon with hole for a MultibandTileRDD") {
      val histogram = multi.polygonalHistogram(polyWithHole)
      val expected = multi.stitch.tile.polygonalHistogram(totalExtent, polyWithHole)

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }
  }

  describe("Histogram Zonal Summary Operation (collections api)") {
    val modHundred = Mod10000TestFile.toCollection
    val multiModHundred = Mod10000TestFile.withContext { _.mapValues { MultibandTile(_) }}
    val ones = AllOnesTestFile.toCollection
    val multi = AllOnesTestFile.withContext { _.mapValues { tile => MultibandTile(tile, tile, tile) }}

    val tileLayout = modHundred.metadata.tileLayout
    val count = modHundred.length * tileLayout.tileCols * tileLayout.tileRows
    val totalExtent = modHundred.metadata.extent

    it("should get correct histogram over whole raster extent") {
      val histogram = modHundred.polygonalHistogram(totalExtent.toPolygon)

      var map = HashMap[Int, Int]()

      for (i <- 0 until count) {
        val key = i % 10000
        val v = map.getOrElse(key, 0) + 1
        map = map + (key -> v)
      }

      map.foreach { case (v, k) => histogram.itemCount(v) should be (k) }
    }

    it("should get correct histogram over whole raster extent for MultibandTiles") {
      val histogram = multiModHundred.polygonalHistogram(totalExtent.toPolygon).head

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

      val histogram = ones.polygonalHistogram(quarterExtent.toPolygon)
      val expected = ones.stitch.tile.polygonalHistogram(totalExtent, quarterExtent.toPolygon)

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

      val histogram = multi.polygonalHistogram(quarterExtent.toPolygon)
      val expected = multi.stitch.tile.polygonalHistogram(totalExtent, quarterExtent.toPolygon)

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

      val poly = Polygon(Line(Array(p1, p2, p3, p4, p1)))

      val histogram = ones.polygonalHistogram(poly)
      val expected = ones.stitch.tile.polygonalHistogram(totalExtent, poly)

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

      val poly = Polygon(Line(Array(p1, p2, p3, p4, p1)))

      val histogram = multi.polygonalHistogram(poly)
      val expected = multi.stitch.tile.polygonalHistogram(totalExtent, poly)

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

      val exterior = Line(Array(pe1, pe2, pe3, pe4, pe1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = Line(Array(pi1, pi2, pi3, pi4, pi1))
      val poly = Polygon(exterior, interior)

      val histogram = ones.polygonalHistogram(poly)
      val expected = ones.stitch.tile.polygonalHistogram(totalExtent, poly)

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

      val exterior = Line(Array(pe1, pe2, pe3, pe4, pe1))

      val pi1 = Point(totalExtent.xmin + xd / 2, totalExtent.ymax - yd / 4)
      val pi2 = Point(totalExtent.xmax - xd / 4, totalExtent.ymin + yd / 2)
      val pi3 = Point(totalExtent.xmin + xd / 2, totalExtent.ymin + yd / 4)
      val pi4 = Point(totalExtent.xmin + xd / 4, totalExtent.ymin + yd / 2)

      val interior = Line(Array(pi1, pi2, pi3, pi4, pi1))
      val poly = Polygon(exterior, interior)

      val histogram = multi.polygonalHistogram(poly)
      val expected = multi.stitch.tile.polygonalHistogram(totalExtent, poly)

      histogram.size should be (expected.size)

      histogram zip expected map { case (result, exp) =>
        result.minMaxValues should be (exp.minMaxValues)
        result.itemCount(1) should be (exp.itemCount(1))
      }
    }
  }
}
