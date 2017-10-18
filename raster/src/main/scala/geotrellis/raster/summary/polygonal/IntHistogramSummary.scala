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
import geotrellis.raster.histogram._
import geotrellis.raster.rasterize._
import geotrellis.vector._


object IntHistogramSummary extends TilePolygonalSummaryHandler[Histogram[Int]] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Histogram[Int] = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    val histogram = FastMapHistogram()
    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) histogram.countItem(z, 1)
    })
    histogram
  }

  def handleFullTile(tile: Tile): Histogram[Int] = {
    val histogram = FastMapHistogram()
    tile.foreach { (z: Int) => if (isData(z)) histogram.countItem(z, 1) }
    histogram
  }

  def combineResults(rs: Seq[Histogram[Int]]): Histogram[Int] =
    if (rs.nonEmpty) rs.reduce(_ merge _)
    else FastMapHistogram()
}

object MultibandTileIntHistogramSummary extends MultibandTilePolygonalSummaryHandler[Array[Histogram[Int]]] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialMultibandTile(raster: Raster[MultibandTile], polygon: Polygon): Array[Histogram[Int]] = {
    val Raster(multibandTile, extent) = raster
    multibandTile.bands.map { tile => IntHistogramSummary.handlePartialTile(Raster(tile, extent), polygon) }.toArray
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullMultibandTile(multibandTile: MultibandTile): Array[Histogram[Int]] =
    multibandTile.bands.map { IntHistogramSummary.handleFullTile(_) }.toArray

  /**
    * Combine the results into a larger result.
    */
  def combineOp(v1: Array[Histogram[Int]], v2: Array[Histogram[Int]]): Array[Histogram[Int]] =
    v1.zipAll(v2, FastMapHistogram(), FastMapHistogram()) map { case (r1, r2) => IntHistogramSummary.combineOp(r1, r2) }

  def combineResults(res: Seq[Array[Histogram[Int]]]): Array[Histogram[Int]] =
    if (res.isEmpty)
      Array(FastMapHistogram())
    else
      res.reduce { (res1, res2) =>
        res1 zip res2 map {
          case (r1: Histogram[Int], r2: Histogram[Int]) =>
            IntHistogramSummary.combineResults(Seq(r1, r2))
        }
      }
}
