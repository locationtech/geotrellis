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
import geotrellis.raster.summary.polygonal.visitors.MeanVisitor
import geotrellis.vector._

import org.scalatest.funspec.AnyFunSpec

class MeanSpec extends AnyFunSpec with TestEnvironment with TestFiles {
  describe("Mean Zonal Summary Operation") {
    val inc = IncreasingTestFile
    val multi = inc.withContext { _.mapValues { tile => MultibandTile(tile, tile) } }

    val tileLayout = inc.metadata.tileLayout
    val count = (inc.count() * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = inc.metadata.extent

    it("should get correct mean over whole raster extent") {
      inc.polygonalSummaryValue(totalExtent.toPolygon(), MeanVisitor).toOption.get.mean should be((count - 1) / 2.0)
    }

    it("should get correct mean over whole raster extent for a MultibandTileRDD") {
      multi.polygonalSummaryValue(totalExtent.toPolygon(), MeanVisitor).toOption.get map { _.mean should be((count - 1) / 2.0) }
    }

    it("should get correct mean over a quarter of the extent") {
      val xd = totalExtent.width
      val yd = totalExtent.height

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )
      val result = inc.polygonalSummaryValue(quarterExtent.toPolygon(), MeanVisitor).toOption.get
      val expected = inc.stitch().polygonalSummary(quarterExtent.toPolygon(), MeanVisitor).toOption.get

      result.mean should be (expected.mean)
    }

    it("should get correct mean over a quarter of the extent for a MultibandTileRDD") {
      val xd = totalExtent.width
      val yd = totalExtent.height

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )
      val result = multi.polygonalSummaryValue(quarterExtent.toPolygon(), MeanVisitor).toOption.get
      val expected = multi.stitch().polygonalSummary(quarterExtent.toPolygon(), MeanVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res.mean should be (exp.mean)
      }
    }
  }

  describe("Mean Zonal Summary Operation (collections api)") {
    val inc = IncreasingTestFile
    val multi = IncreasingTestFile.withContext { _.mapValues { tile => MultibandTile(tile, tile) } }

    val tileLayout = inc.metadata.tileLayout
    val count = inc.toCollection.length * tileLayout.tileCols * tileLayout.tileRows
    val totalExtent = inc.metadata.extent

    it("should get correct mean over whole raster extent") {
      inc.polygonalSummaryValue(totalExtent.toPolygon(), MeanVisitor).toOption.get.mean should be((count - 1) / 2.0)
    }

    it("should get correct mean over whole raster extent for MultibandTiles") {
      multi.polygonalSummaryValue(totalExtent.toPolygon(), MeanVisitor).toOption.get map { _.mean should be((count - 1) / 2.0) }
    }

    it("should get correct mean over a quarter of the extent") {
      val xd = totalExtent.width
      val yd = totalExtent.height

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )
      val result = inc.polygonalSummaryValue(quarterExtent.toPolygon(), MeanVisitor).toOption.get
      val expected = inc.stitch().polygonalSummary(quarterExtent.toPolygon(), MeanVisitor).toOption.get

      result.mean should be (expected.mean)
    }

    it("should get correct mean over a quarter of the extent for MultibandTiles") {
      val xd = totalExtent.width
      val yd = totalExtent.height

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )
      val result = multi.polygonalSummaryValue(quarterExtent.toPolygon(), MeanVisitor).toOption.get
      val expected = multi.stitch().polygonalSummary(quarterExtent.toPolygon(), MeanVisitor).toOption.get

      result.size should be (expected.size)

      result zip expected map { case (res, exp) =>
        res.mean should be (exp.mean)
      }
    }
  }
}
