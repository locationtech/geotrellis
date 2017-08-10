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
