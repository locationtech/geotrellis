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
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch.Stitcher
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.collection.mutable.{Map => MuMap}

object TileLayoutStitcher {
  /**
   * Stitches a collection of tiles keyed by (col, row) tuple into a single tile.
   * Makes the assumption that all tiles are of equal size such that they can be placed in a grid layout.
   * @return A tuple of the stitched tile and the GridBounds of keys used to construct it.
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

  /**
    * Stitches a collection of spatially-keyed tiles into a single tile.  Assumes that all
    * tiles in a given column (row) have the same width (height).
    *
    * @param    tiles       A traversable collection of (key, tile) pairs
    * @return               The stitched tile
    */
  def genericStitch[
    V <: CellGrid: ? => TilePrototypeMethods[V]: ? => TileMergeMethods[V]
  ](tiles: Traversable[(Product2[Int, Int], V)]) = {
    assert(tiles.size > 0, "Cannot stitch empty collection")

    val colWidths = MuMap.empty[Int, Int]
    val rowHeights = MuMap.empty[Int, Int]
    tiles.foreach{ case (key, tile) =>
      val curWidth = colWidths.getOrElseUpdate(key._1, tile.cols)
      assert(curWidth == tile.cols, "Tiles in a layout column must have the same width")
      val curHeight = rowHeights.getOrElseUpdate(key._2, tile.rows)
      assert(curHeight == tile.rows, "Tiles in a layout row must have the same height")
    }

    val (colPos, width) = colWidths.toSeq.sorted.foldLeft( (Map.empty[Int, Int], 0) ){
      case ((positions, acc), (col, w)) => (positions + (col -> acc), acc + w)
    }
    val (rowPos, height) = rowHeights.toSeq.sorted.foldLeft( (Map.empty[Int, Int], 0) ){
      case ((positions, acc), (row, h)) => (positions + (row -> acc), acc + h)
    }

    val result = tiles.head._2.prototype(width, height)
    tiles.foreach{ case (key, tile) => {
      result.merge(tile, colPos(key._1), rowPos(key._2))
    }}

    result
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

  def genericStitch()(implicit proto: V => TilePrototypeMethods[V], merge: V => TileMergeMethods[V]): V = {
    TileLayoutStitcher.genericStitch(self.collect)
  }
}
