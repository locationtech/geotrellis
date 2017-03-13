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
