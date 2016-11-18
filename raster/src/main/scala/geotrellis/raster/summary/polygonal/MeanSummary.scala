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


case class MeanResult(sum: Double, count: Long) {
  def mean: Double = if (count == 0) {
    Double.NaN
  } else {
    sum/count
  }
  def +(b: MeanResult) = MeanResult(sum + b.sum, count + b.count)
}

object MeanResult {
  def fromFullTile(tile: Tile) = {
    var s = 0
    var c = 0L
    tile.foreach((x: Int) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanResult(s, c)
  }

  def fromFullTileDouble(tile: Tile) = {
    var s = 0.0
    var c = 0L
    tile.foreachDouble((x: Double) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanResult(s, c)
  }
}

object MeanSummary extends TilePolygonalSummaryHandler[MeanResult] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): MeanResult = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum = 0.0
    var count = 0L
    if(tile.cellType.isFloatingPoint) {
      polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
        val z = tile.getDouble(col, row)
        if (isData(z)) { sum = sum + z; count = count + 1 }
      })
      } else {
        polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
          val z = tile.get(col, row)
          if (isData(z)) { sum = sum + z; count = count + 1 }
        })
      }

    MeanResult(sum, count)
  }

  def handleFullTile(tile: Tile): MeanResult =
    if(tile.cellType.isFloatingPoint) {
      MeanResult.fromFullTileDouble(tile)
    } else {
      MeanResult.fromFullTile(tile)
    }

  def combineResults(rs: Seq[MeanResult]): MeanResult =
    rs.foldLeft(MeanResult(0.0, 0L))(_+_)
}
