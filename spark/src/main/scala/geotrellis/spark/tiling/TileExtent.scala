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

trait TileIdTransform {
  def tileId(tileCoord: TileCoord): TileId =
    tileId(tileCoord._1, tileCoord._2)

  def tileId(tx: Int, ty: Int): TileId
  def tileCoord(tileId: TileId): TileCoord
}

/**
 * Span of consecutive tile IDs, inclusive
 * @param min minimum TMS Tile ID in the span
 * @param max maximum TMS Tile ID in the span
 */
case class TileSpan(min: Long, max: Long)

object TileExtent {
  def apply(xmin: Int, ymin: Int, xmax: Int, ymax: Int)(tileTransform: TileIdTransform): TileExtent =
    new TileExtent(xmin, ymin, xmax, ymax)(tileTransform)
}

/* Represents the dimensions of the tiles of a RasterRDD
 * based on a zoom level and world grid.
 */
class TileExtent(val xmin: Int, val ymin: Int, val xmax: Int, val ymax: Int)
                (tileTransform: TileIdTransform) {
  lazy val width = (xmax - xmin + 1)
  lazy val height = (ymax - ymin + 1)

  lazy val tileIds: Array[TileId] = {
    val arr = Array.ofDim[TileId](width*height)
    cfor(0)(_ < height, _ + 1) { row =>
      cfor(0)(_ < width, _ + 1) { col =>
        arr(row * width + col) = 
          tileTransform.tileId(col + xmin, row + ymin)
      }
    }
    arr
  }

  /**
   * Return a range from min tileId to max tileID for every row in the extent
   */
  def rowSpans: Seq[TileSpan] =
    for (y <- ymin to ymax) yield 
      TileSpan(tileTransform.tileId(xmin, y), tileTransform.tileId(xmax, y))

  def contains(tileId: Long) = {
    val (x, y) = tileTransform.tileCoord(tileId)
    (x <= xmax && x >= xmin) && (y <= ymax && y >= ymin)
  }

  override
  def hashCode = (xmin, ymin, xmax, ymax).hashCode

  override
  def equals(o: Any): Boolean =
    o match {
      case other: TileExtent =>
        (other.xmin, other.ymin, other.xmax, other.ymax)
          .equals((xmin, ymin, xmax, ymax))
      case _ => false
    }
} 
