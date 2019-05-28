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

package geotrellis.layers.mapalgebra.focal

import geotrellis.tiling.{SpatialComponent, TileBounds}
import geotrellis.raster.buffer.BufferedTile
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.layers._
import geotrellis.util.MethodExtensions

object CollectionFocalOperation {
  private def mapOverBufferedTiles[K: SpatialComponent](
    bufferedTiles: Seq[(K, BufferedTile[Tile])],
    neighborhood: Neighborhood
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  ): Seq[(K, Tile)] =
    bufferedTiles
      .map { case (key, BufferedTile(tile, gridBounds)) => key -> calc(tile, Some(gridBounds)) }

  def apply[K: SpatialComponent](
    seq: Seq[(K, Tile)],
    neighborhood: Neighborhood
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  )(
    implicit d: DummyImplicit
  ): Seq[(K, Tile)] =
    mapOverBufferedTiles(seq.bufferTiles(neighborhood.extent), neighborhood)(calc)

  def apply[K: SpatialComponent](
    rdd: Seq[(K, Tile)],
    neighborhood: Neighborhood,
    layerBounds: TileBounds
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  ): Seq[(K, Tile)] =
    mapOverBufferedTiles(rdd.bufferTiles(neighborhood.extent, layerBounds), neighborhood)(calc)

  def apply[K: SpatialComponent](
    rasterCollection: TileLayerCollection[K],
    neighborhood: Neighborhood
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  ): TileLayerCollection[K] =
    rasterCollection.withContext { rdd =>
      apply(rdd, neighborhood, rasterCollection.metadata.tileBounds)(calc)
    }
}

abstract class CollectionFocalOperation[K: SpatialComponent] extends MethodExtensions[TileLayerCollection[K]] {

  def focal(n: Neighborhood)(calc: (Tile, Option[GridBounds[Int]]) => Tile): TileLayerCollection[K] =
    CollectionFocalOperation(self, n)(calc)

  def focalWithCellSize(n: Neighborhood)(calc: (Tile, Option[GridBounds[Int]], CellSize) => Tile): TileLayerCollection[K] = {
    val cellSize = self.metadata.layout.cellSize
    CollectionFocalOperation(self, n){ (tile, bounds) =>
      calc(tile, bounds, cellSize)
    }
  }
}
