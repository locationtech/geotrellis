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

package geotrellis.layer.stitch

import geotrellis.raster._
import geotrellis.raster.stitch.Stitcher
import geotrellis.layer._
import geotrellis.raster.prototype._
import geotrellis.vector._
import geotrellis.util._

abstract class SpatialTileLayoutCollectionStitchMethods[
  V <: CellGrid[Int]: Stitcher: * => TilePrototypeMethods[V],
  M: GetComponent[*, LayoutDefinition]
] extends MethodExtensions[Seq[(SpatialKey, V)] with Metadata[M]] {

  def stitch(): Raster[V] = {
    val (tile, (kx, ky), (offsx, offsy)) = TileLayoutStitcher.stitch(self)
    val layout = self.metadata.getComponent[LayoutDefinition]
    val mapTransform = layout.mapTransform
    val nwTileEx = mapTransform(kx, ky)
    val base = nwTileEx.southEast
    val (ulx, uly) = (base.x - offsx.toDouble * layout.cellwidth, base.y + offsy * layout.cellheight)
    Raster(tile, Extent(ulx, uly - tile.rows * layout.cellheight, ulx + tile.cols * layout.cellwidth, uly))
  }

  /**
    * Stitch all tiles in the collection using a sparse stitch that handles missing keys
    *
    * Any missing tiles within the extent are filled with an empty prototype tile.
    *
    * @param extent The requested [[geotrellis.vector.Extent]] of the output [[geotrellis.raster.Raster]]
    * @return The stitched Raster, otherwise None if the collection is empty or the extent does not intersect
    */
  def sparseStitch(extent: Extent): Option[Raster[V]] = {
    if (self.isEmpty) {
      None
    } else {
      val tile = self.head._2
      val layoutDefinition = self.metadata.getComponent[LayoutDefinition]
      val mapTransform = layoutDefinition.mapTransform
      val expectedKeys = mapTransform(extent)
        .coordsIter
        .map { case (x, y) => SpatialKey(x, y) }
        .toList
      val actualKeys = self.map(_._1)
      val missingKeys = expectedKeys diff actualKeys

      val missingTiles = missingKeys.map { key =>
        (key, tile.prototype(layoutDefinition.tileLayout.tileCols, layoutDefinition.tileLayout.tileRows))
      }
      val allTiles = self.withContext { collection =>
        collection ++ missingTiles
      }
      if (allTiles.isEmpty) {
        None
      } else {
        Some(allTiles.stitch())
      }
    }
  }

  /**
    * sparseStitch helper method that uses the extent of the collection it is called on
    *
    * @see sparseStitch(extent: Extent) for more details
    */
  def sparseStitch(): Option[Raster[V]] = {
    val layoutDefinition = self.metadata.getComponent[LayoutDefinition]
    sparseStitch(layoutDefinition.extent)
  }
}

abstract class SpatialTileCollectionStitchMethods[V <: CellGrid[Int]: Stitcher]
  extends MethodExtensions[Seq[(SpatialKey, V)]] {

  def stitch(): V = TileLayoutStitcher.stitch(self)._1
}
