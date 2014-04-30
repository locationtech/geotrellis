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

import geotrellis.RasterType
import geotrellis.Extent

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

  def tileId(tx: Long, ty: Long, zoom: Int) = (ty * numXTiles(zoom)) + tx
  
  def tileXY(tileId: Long, zoom: Int): (Long, Long) = {
    val width = numXTiles(zoom)
    val ty = tileId / width
    val tx = tileId - (ty * width)
    (tx, ty)
  }

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
  
  def tileToExtent(te: TileExtent, zoom: Int, tileSize: Int): Extent = {
    val ll = tileToExtent(te.xmin, te.ymin, zoom, tileSize)
    val ur = tileToExtent(te.xmax, te.ymax, zoom, tileSize)
    Extent(ll.xmin, ll.ymin, ur.xmax, ur.ymax)
  }
  
  def extentToTile(extent: Extent, zoom: Int, tileSize: Int): TileExtent = {
    val ll = latLonToTile(extent.ymin, extent.xmin, zoom, tileSize)
    val ur = latLonToTile(extent.ymax, extent.xmax, zoom, tileSize)
    new TileExtent(ll.tx, ll.ty, ur.tx, ur.ty)
  }

  def extentToPixel(extent: Extent, zoom: Int, tileSize: Int): PixelExtent = {
    val (pixelLower, pixelUpper) =
      (TmsTiling.latLonToPixels(extent.ymin, extent.xmin, zoom, tileSize),
        TmsTiling.latLonToPixels(extent.ymax, extent.xmax, zoom, tileSize))
    PixelExtent(0, 0, pixelUpper.px - pixelLower.px, pixelUpper.py - pixelLower.py)    
  }
  
  def latLonToPixels(lat: Double, lon: Double, zoom: Int, tileSize: Int): Pixel = {
    val res = resolution(zoom, tileSize)
    new Pixel(((180 + lon) / res).toLong, ((90 + lat) / res).toLong)
  }

  def pixelsToTile(px: Double, py: Double, tileSize: Int): TileCoord = {
    new TileCoord((px / tileSize).toLong, (py / tileSize).toLong)
  }

  // slightly modified version of equations 2.9 and 2.10
  def latLonToTile(lat: Double, lon: Double, zoom: Int, tileSize: Int): TileCoord = {
    val tx = ((180 + lon) * (numXTiles(zoom) / 360.0)).toLong
    val ty = ((90 + lat) * (numYTiles(zoom) / 180.0)).toLong
    new TileCoord(tx, ty)
  }

  def tileSizeBytes(tileSize: Int, rasterType: RasterType): Int = 
    tileSize * tileSize * rasterType.bytes
}
