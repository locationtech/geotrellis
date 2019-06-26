/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.summary.polygonal.visitors

import cats.Monoid
import geotrellis.raster._
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.summary.GridVisitor

/**
  * Visitor that constructs a StreamingHistogram of visited values
  *
  * Each visited value is given a weight of 1 in the histogram.
  *
  * Implementations provided for Raster[(Tile, MultibandTile)]
  *
  * This visitor skips row/col positions with NaN values.
  */
object StreamingHistogramVisitor {
  implicit def toTileVisitor(t: StreamingHistogramVisitor.type):
    TileStreamingHistogramVisitor = new TileStreamingHistogramVisitor
  implicit def toMultibandTileVisitor(t: StreamingHistogramVisitor.type):
    MultibandTileStreamingHistogramVisitor = new MultibandTileStreamingHistogramVisitor

  class TileStreamingHistogramVisitor
      extends GridVisitor[Raster[Tile], StreamingHistogram] {
    private val accumulator = Monoid[StreamingHistogram].empty

    def result: StreamingHistogram = accumulator

    def visit(raster: Raster[Tile], col: Int, row: Int): Unit = {
      val v = raster.tile.getDouble(col, row)
      if (isData(v)) accumulator.countItem(v, count = 1)
    }
  }

  class MultibandTileStreamingHistogramVisitor
      extends GridVisitor[Raster[MultibandTile], Array[StreamingHistogram]] {
    private var accumulator = Array[StreamingHistogram]()
    private var initialized = false

    def result: Array[StreamingHistogram] = accumulator

    def visit(raster: Raster[MultibandTile], col: Int, row: Int): Unit = {
      val tiles = raster.tile.bands.toArray
      if (!initialized) {
        accumulator = Array.fill[StreamingHistogram](tiles.size)(Monoid[StreamingHistogram].empty)
        initialized = true
      }
      tiles.zip(accumulator).foreach {
        case (tile: Tile, histogram: StreamingHistogram) => {
          val newValue = tile.getDouble(col, row)
          if (isData(newValue)) histogram.countItem(newValue, count = 1)
        }
      }
    }
  }
}
