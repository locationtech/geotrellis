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

object MultibandTileMeanSummary extends MultibandTilePolygonalSummaryHandler[Array[MeanResult]] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialMultibandTile(raster: Raster[MultibandTile], polygon: Polygon): Array[MeanResult] = {
    val Raster(multibandTile, extent) = raster
    multibandTile.bands.map { tile => MeanSummary.handlePartialTile(Raster(tile, extent), polygon) }.toArray
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullMultibandTile(multibandTile: MultibandTile): Array[MeanResult] =
    multibandTile.bands.map { MeanSummary.handleFullTile(_) }.toArray

  /**
    * Combine the results into a larger result.
    */
  def combineOp(v1: Array[MeanResult], v2: Array[MeanResult]): Array[MeanResult] =
    v1.zipAll(v2, MeanResult(0.0, 0L), MeanResult(0.0, 0L)) map { case (r1, r2) => MeanSummary.combineOp(r1, r2) }

  def combineResults(res: Seq[Array[MeanResult]]): Array[MeanResult] =
    if (res.isEmpty)
      Array(MeanResult(0.0, 0L))
    else
      res.reduce { (res1, res2) =>
        res1 zip res2 map {
          case (r1: MeanResult, r2: MeanResult) =>
            MeanSummary.combineResults(Seq(r1, r2))
        }
      }
}
