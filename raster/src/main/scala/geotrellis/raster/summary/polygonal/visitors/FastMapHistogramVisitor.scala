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
import cats.syntax.monoid._
import geotrellis.raster._
import geotrellis.raster.histogram.FastMapHistogram
import geotrellis.raster.summary.GridVisitor

/**
  * Visitor that constructs a FastMapHistogram of visited values
  *
  * Each visited value is given a weight of 1 in the histogram.
  *
  * Implementations provided for Raster[(Tile, MultibandTile)]
  *
  * This visitor skips row/col positions with NaN values.
  *
  * NOTE: This visitor is provided as a convenience for small datasets
  *       and for testing. For larger applications you almost certainly
  *       want StreamingHistogramVisitor which can handle unbounded
  *       inputs.
  */
object FastMapHistogramVisitor {
  implicit def toTileVisitor(t: FastMapHistogramVisitor.type):
  TileFastMapHistogramVisitor = new TileFastMapHistogramVisitor
  implicit def toMultibandTileVisitor(t: FastMapHistogramVisitor.type):
  MultibandTileFastMapHistogramVisitor = new MultibandTileFastMapHistogramVisitor

  class TileFastMapHistogramVisitor
    extends GridVisitor[Raster[Tile], FastMapHistogram] {
    private val accumulator = Monoid[FastMapHistogram].empty

    def result: FastMapHistogram = accumulator

    def visit(raster: Raster[Tile], col: Int, row: Int): Unit = {
      val v = raster.tile.get(col, row)
      if (isData(v)) accumulator.countItem(v, count = 1)
    }
  }

  class MultibandTileFastMapHistogramVisitor
    extends GridVisitor[Raster[MultibandTile], Array[FastMapHistogram]] {
    private var accumulator = Array[FastMapHistogram]()
    private var initialized = false

    def result: Array[FastMapHistogram] = accumulator

    def visit(raster: Raster[MultibandTile], col: Int, row: Int): Unit = {
      val tiles = raster.tile.bands.toArray
      if (!initialized) {
        accumulator = Array.fill[FastMapHistogram](tiles.size)(Monoid[FastMapHistogram].empty)
        initialized = true
      }
      tiles.zip(accumulator).foreach {
        case (tile: Tile, histogram: FastMapHistogram) => {
          val newValue = tile.get(col, row)
          if (isData(newValue)) histogram.countItem(newValue, count = 1)
        }
      }
    }
  }
}

