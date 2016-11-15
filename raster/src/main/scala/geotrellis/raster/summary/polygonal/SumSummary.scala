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


/**
  * Object containing functions for doing sum operations on
  * [[Raster]]s.
  */
object SumSummary extends TilePolygonalSummaryHandler[Long] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Long = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum: Long = 0L

    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) { sum = sum + z }
    })

    sum
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullTile(tile: Tile): Long = {
    var s = 0L
    tile.foreach { (x: Int) => if (isData(x)) s = s + x }
    s
  }

  /**
    * Combine the results into a larger result.
    */
  def combineResults(rs: Seq[Long]) =
    rs.foldLeft(0L)(_+_)
}

/**
  * Object containing functions for doing sum operations on
  * [[Raster]]s.
  */
object SumDoubleSummary extends TilePolygonalSummaryHandler[Double] {

  /**
    * Given a [[Raster]] which partially intersects the given polygon,
    * find the sum of the Raster elements in the intersection.
    */
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Double = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum = 0.0

    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if(isData(z)) { sum = sum + z }
    })

    sum
  }

  /**
    * Find the sum of the elements in the [[Raster]].
    */
  def handleFullTile(tile: Tile): Double = {
    var s = 0.0
    tile.foreachDouble((x: Double) => if (isData(x)) s = s + x)
    s
  }

  /**
    * Combine the results into a larger result.
    */
  def combineResults(rs: Seq[Double]) =
    rs.foldLeft(0.0)(_+_)
}
