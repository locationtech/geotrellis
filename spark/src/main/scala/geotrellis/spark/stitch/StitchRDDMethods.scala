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

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch.Stitcher
import geotrellis.tiling._
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.util._
import org.apache.spark.rdd.RDD

import scala.collection.mutable

object TileLayoutStitcher {
  /**
    * Stitches a collection of spatially-keyed tiles into a single tile.  Assumes that all
    * tiles in a given column (row) have the same width (height).
    *
    * @param    tiles       A traversable collection of (key, tile) pairs
    * @return               A tuple with the stitched tile, the key of the upper left tile
    *                       of the layout, and the width and height of that spatial key
    */
  def stitch[
    V <: CellGrid[Int]: Stitcher
  ](tiles: Traversable[(Product2[Int, Int], V)]): (V, (Int, Int), (Int, Int)) = {
    assert(tiles.size > 0, "Cannot stitch empty collection")

    val colWidths = mutable.Map.empty[Int, Int]
    val rowHeights = mutable.Map.empty[Int, Int]
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

    // val result = tiles.head._2.prototype(width, height)
    // tiles.foreach{ case (key, tile) => {
    //   result.merge(tile, colPos(key._1), rowPos(key._2))
    // }}

    val stitcher = implicitly[Stitcher[V]]
    val result = stitcher.stitch(tiles.map{ case (key, tile) => tile -> (colPos(key._1), rowPos(key._2)) }.toIterable, width, height)

    val (minx, miny) = (colWidths.keys.min, rowHeights.keys.min)
    (result, (minx, miny), (colWidths(minx), rowHeights(miny)))
  }
}

abstract class SpatialTileLayoutRDDStitchMethods[
  V <: CellGrid[Int]: Stitcher,
  M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[RDD[(SpatialKey, V)] with Metadata[M]] {

  def stitch(): Raster[V] = {
    val (tile, (kx, ky), (offsx, offsy)) = TileLayoutStitcher.stitch(self.collect())
    val layout = self.metadata.getComponent[LayoutDefinition]
    val mapTransform = layout.mapTransform
    val nwTileEx = mapTransform(kx, ky)
    val base = nwTileEx.southEast
    val (ulx, uly) = (base.x - offsx.toDouble * layout.cellwidth, base.y + offsy * layout.cellheight)
    Raster(tile, Extent(ulx, uly - tile.rows * layout.cellheight, ulx + tile.cols * layout.cellwidth, uly))
  }
}

abstract class SpatialTileRDDStitchMethods[V <: CellGrid[Int]: Stitcher]
  extends MethodExtensions[RDD[(SpatialKey, V)]] {

  def stitch(): V = {
    TileLayoutStitcher.stitch(self.collect())._1
  }
}
