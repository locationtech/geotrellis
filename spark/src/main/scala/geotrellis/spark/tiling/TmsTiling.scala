/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.vector.Extent

/**
 * @author akini
 *
 * A TMS based tiling scheme taken from this book:
 *
 * "Tile-Based Geospatial Information Systems Principles and Practices"
 * by John T. Sample • Elias Ioup
 *
 * Tiles are indexed by their column and row identifiers - tx and ty, which start from (0,0)
 * on the lower left corner of the world and go upto numXTiles-1 and numYTiles-1 respectively
 * (see below for their implementations)
 *
 *
 */
object TmsTiling {

  val Epsilon = 0.00000001
  val MaxZoomLevel = 22
  val DefaultTileSize = 512

  def numXTiles(zoom: Int) = math.pow(2, zoom).toLong
  def numYTiles(zoom: Int) = math.pow(2, zoom - 1).toLong

  def tileId(tx: Long, ty: Long, zoom: Int): Long = ???
  
  def tileXY(tileId: Long, zoom: Int): (Long, Long) = ???

  def resolution(zoom: Int, tileSize: Int): Double = 
    360 / (numXTiles(zoom) * tileSize).toDouble

  def zoom(res: Double, tileSize: Int): Int = {
    val resWithEp = res + Epsilon

    for(i <- 1 to MaxZoomLevel) {
      if(resWithEp >= resolution(i, tileSize))
        return i
    }
    return 0
  }

  // using equations 2.3 through 2.6 from TBGIS book
  def tileToExtent(tx: Long, ty: Long, zoom: Int, tileSize: Int): Extent = {
    val res = resolution(zoom, tileSize)
    Extent(tx * tileSize * res - 180, // left/west (lon, x)
      ty * tileSize * res - 90, // lower/south (lat, y)
      (tx + 1) * tileSize * res - 180, // right/east (lon, x)
      (ty + 1) * tileSize * res - 90) // upper/north (lat, y)
  }

  def tileToExtent(tileId: Long, zoom: Int, tileSize: Int): Extent = {
    val (tx,ty) = tileXY(tileId, zoom)
    tileToExtent(tx, ty, zoom, tileSize)
  }
}
