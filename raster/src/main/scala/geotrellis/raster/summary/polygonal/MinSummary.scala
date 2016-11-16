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
import geotrellis.raster.rasterize._
import geotrellis.vector._


object MinSummary extends TilePolygonalSummaryHandler[Int] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Int = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var min = NODATA
    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z) && (z < min || isNoData(min)) ) { min = z }
    })
    min
  }

  def handleFullTile(tile: Tile): Int = {
    var min = NODATA
    tile.foreach { (x: Int) =>
      if (isData(x) && (x < min || isNoData(min))) { min = x }
    }
    min
  }

  def combineResults(rs: Seq[Int]): Int =
    if(rs.isEmpty) NODATA
    else
      rs.reduce { (a, b) =>
        if(isNoData(a)) { b }
        else if(isNoData(b)) { a }
        else { math.min(a, b) }
      }
}

object MinDoubleSummary extends TilePolygonalSummaryHandler[Double] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Double = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var min = Double.NaN
    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if (isData(z) && (z < min || isNoData(min))) { min = z }
    })

    min
  }

  def handleFullTile(tile: Tile): Double = {
    var min = Double.NaN
    tile.foreachDouble { (x: Double) =>
      if (isData(x) && (x < min || isNoData(min))) { min = x }
    }
    min
  }

  def combineResults(rs: Seq[Double]): Double =
    if(rs.isEmpty) Double.NaN
    else
      rs.reduce { (a, b) =>
        if(isNoData(a)) { b }
        else if(isNoData(b)) { a }
        else { math.min(a, b) }
      }
}
