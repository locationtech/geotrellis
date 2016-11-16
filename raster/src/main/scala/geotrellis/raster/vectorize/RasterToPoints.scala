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

package geotrellis.raster.vectorize

import geotrellis.raster._
import geotrellis.vector.{Point, PointFeature, _}

import scala.collection.mutable.ArrayBuffer

object RasterToPoints {

  def fromInt(tile: Tile, extent: Extent) = {
    val cols = tile.cols
    val rows = tile.rows

    val points = ArrayBuffer.empty[PointFeature[Int]]
    val rasterExtent = RasterExtent(extent, cols, rows)

    for (col <- 0 until cols; row <- 0 until rows) {
      val value = tile.get(col, row)

      if (!isNoData(value)) {
        val (x, y) = rasterExtent.gridToMap(col, row)

        points += PointFeature(Point(x, y), value)
      }
    }

    points
  }

  def fromDouble(tile: Tile, extent: Extent) = {
    val cols = tile.cols
    val rows = tile.rows

    val points = ArrayBuffer.empty[PointFeature[Double]]
    val rasterExtent = RasterExtent(extent, cols, rows)

    for (col <- 0 until cols; row <- 0 until rows) {
      val value = tile.getDouble(col, row)

      if (!isNoData(value)) {
        val (x, y) = rasterExtent.gridToMap(col, row)

        points += PointFeature(Point(x, y), value)
      }
    }

    points
  }
}
