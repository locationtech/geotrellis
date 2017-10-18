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
import geotrellis.vector._
import geotrellis.raster.rasterize._
import geotrellis.raster.histogram._

object DoubleHistogramSummary extends TilePolygonalSummaryHandler[Histogram[Double]] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Histogram[Double] = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    val histogram = StreamingHistogram()
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if (isData(z)) histogram.countItem(z, 1)
    }
    histogram
  }

  def handleFullTile(tile: Tile): Histogram[Double] = {
    val histogram = StreamingHistogram()
    tile.foreach { (z: Int) => if (isData(z)) histogram.countItem(z, 1) }
    histogram
  }

  def combineResults(rs: Seq[Histogram[Double]]): Histogram[Double] =
    if (rs.nonEmpty) rs.reduce(_ merge _)
    else StreamingHistogram()
}

object MultibandTileDoubleHistogramSummary extends MultibandTilePolygonalSummaryHandler[Array[Histogram[Double]]] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialMultibandTile(raster: Raster[MultibandTile], polygon: Polygon): Array[Histogram[Double]] = {
    val Raster(multibandTile, extent) = raster
    multibandTile.bands.map { tile => DoubleHistogramSummary.handlePartialTile(Raster(tile, extent), polygon) }.toArray
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullMultibandTile(multibandTile: MultibandTile): Array[Histogram[Double]] =
    multibandTile.bands.map { DoubleHistogramSummary.handleFullTile(_) }.toArray

  /**
    * Combine the results into a larger result.
    */
  def combineOp(v1: Array[Histogram[Double]], v2: Array[Histogram[Double]]): Array[Histogram[Double]] =
    v1.zipAll(v2, StreamingHistogram(), StreamingHistogram()) map { case (r1, r2) => DoubleHistogramSummary.combineOp(r1, r2) }

  def combineResults(res: Seq[Array[Histogram[Double]]]): Array[Histogram[Double]] =
    if (res.isEmpty)
      Array(StreamingHistogram())
    else
      res.reduce { (res1, res2) =>
        res1 zip res2 map {
          case (r1: Histogram[Double], r2: Histogram[Double]) =>
            DoubleHistogramSummary.combineResults(Seq(r1, r2))
        }
      }
}
