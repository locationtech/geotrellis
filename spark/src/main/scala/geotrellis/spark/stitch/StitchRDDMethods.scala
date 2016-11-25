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

package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.raster.stitch.Stitcher
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD


object TileLayoutStitcher {
  /**
   * Stitches a collection of tiles keyed by (col, row) tuple into a single tile.
   * Makes the assumption that all tiles are of equal size such that they can be placed in a grid layout.
   * @return An option of stitched tile and the GridBounds of keys used to construct it.
   */
  def stitch[V <: CellGrid](tiles: Iterable[(Product2[Int, Int], V)])
  (implicit stitcher: Stitcher[V]): (V, GridBounds) = {
    require(tiles.nonEmpty, "nonEmpty input")
    val sample = tiles.head._2
    val te = GridBounds.envelope(tiles.map(_._1))
    val tileCols = sample.cols
    val tileRows = sample.rows

    val pieces =
      for ((SpatialKey(col, row), v) <- tiles) yield {
        val updateCol = (col - te.colMin) * tileCols
        val updateRow = (row - te.rowMin) * tileRows
        (v, (updateCol, updateRow))
      }
    val tile: V = stitcher.stitch(pieces, te.width * tileCols, te.height * tileRows)
    (tile, te)
  }
}

abstract class SpatialTileLayoutRDDStitchMethods[V <: CellGrid: Stitcher, M: GetComponent[?, LayoutDefinition]]
  extends MethodExtensions[RDD[(SpatialKey, V)] with Metadata[M]] {

  def stitch(): Raster[V] = {
    val (tile, bounds) = TileLayoutStitcher.stitch(self.collect())
    val mapTransform = self.metadata.getComponent[LayoutDefinition].mapTransform
    Raster(tile, mapTransform(bounds))
  }
}

abstract class SpatialTileRDDStitchMethods[V <: CellGrid: Stitcher]
  extends MethodExtensions[RDD[(SpatialKey, V)]] {

  def stitch(): V = {
    TileLayoutStitcher.stitch(self.collect())._1
  }
}
