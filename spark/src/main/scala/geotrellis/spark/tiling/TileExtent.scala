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

import geotrellis.spark._
import geotrellis.vector.Extent

import spire.syntax.cfor._

case class TileCoord(tx: Int, ty: Int)

/**
 * Span of consecutive tile IDs, inclusive
 * @param min minimum TMS Tile ID in the span
 * @param max maximum TMS Tile ID in the span
 */
case class TileSpan(min: Long, max: Long)

/* Represents the dimensions of the tiles of a RasterRDD
 * based on a zoom level and world grid.
 */
case class TileExtent(xmin: Int, ymin: Int, xmax: Int, ymax: Int) {
  private def coordinateToId(col: Int, row: Int, zoomLevel: ZoomLevel): TileId = {
    zoomLevel.tileId(col, row)
  }

  lazy val width = (xmax - xmin + 1)
  lazy val height = (ymax - ymin + 1)

  def tiles(zoomLevel: ZoomLevel): Array[TileId] = {
    val arr = Array.ofDim[TileId](width*height)
    cfor(0)(_ < height, _ + 1) { row =>
      cfor(0)(_ < width, _ + 1) { col =>
        arr(row * width + col) = coordinateToId(col + xmin, row + ymin, zoomLevel)
      }
    }
    arr
  }

  /**
   * Return a range from min tileId to max tileID for every row in the extent
   */
  def rowRanges(zoomLevel: ZoomLevel): Seq[TileSpan] =
    for (y <- ymin to ymax) yield 
      TileSpan(zoomLevel.tileId(xmin, y), zoomLevel.tileId(xmax, y))

  def contains(zoomLevel: ZoomLevel)(tileId: Long) = {
    val (x, y) = zoomLevel.tileXY(tileId)
    (x <= xmax && x >= xmin) && (y <= ymax && y >= ymin)
  }

  def foreach(zoomLevel: ZoomLevel)(f: Long => Unit): Unit = 
    tiles(zoomLevel).foreach(f)

  def map[T](zoomLevel: ZoomLevel)(f: Long => T): Seq[T] = 
    tiles(zoomLevel).map(f)

  def count(zoomLevel: ZoomLevel): Long = 
    tiles(zoomLevel).size
} 
