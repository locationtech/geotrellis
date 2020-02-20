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

package geotrellis.layer.mapalgebra.focal

import geotrellis.layer._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.buffer.BufferedTile
import geotrellis.raster.mapalgebra.focal._
import geotrellis.util._


object CollectionFocalOperation {
  private def mapOverBufferedTiles[K: SpatialComponent](
    bufferedTiles: Seq[(K, BufferedTile[Tile])],
    neighborhood: Neighborhood
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  ): Seq[(K, Tile)] =
    bufferedTiles
      .mapValues { case BufferedTile(tile, gridBounds) => calc(tile, Some(gridBounds)) }

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
    collection: Seq[(K, Tile)],
    neighborhood: Neighborhood,
    layerBounds: TileBounds
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  ): Seq[(K, Tile)] =
    mapOverBufferedTiles(collection.bufferTiles(neighborhood.extent, layerBounds), neighborhood)(calc)

  def apply[K: SpatialComponent](
    rasterCollection: TileLayerCollection[K],
    neighborhood: Neighborhood
  )(
    calc: (Tile, Option[GridBounds[Int]]) => Tile
  ): TileLayerCollection[K] =
    rasterCollection.withContext { collection =>
      apply(collection, neighborhood, rasterCollection.metadata.tileBounds)(calc)
    }

  private def applyOnRaster[K: SpatialComponent: GetComponent[*, SpatialKey]](
    bufferedTiles: Seq[(K, BufferedTile[Tile])],
    neighborhood: Neighborhood,
    keyToExtent: SpatialKey => Extent
  )(
    calc: (Raster[Tile], Option[GridBounds[Int]]) => Tile
  ): Seq[(K, Tile)] =
    bufferedTiles
      .map { case (key, BufferedTile(tile, gridBounds)) =>
        val spatialKey = key.getComponent[SpatialKey]

        key -> calc(Raster(tile, keyToExtent(spatialKey)), Some(gridBounds))
      }

  def applyOnRaster[K: SpatialComponent: GetComponent[*, SpatialKey]](
    collection: Seq[(K, Tile)],
    neighborhood: Neighborhood,
    layerBounds: TileBounds,
    keyToExtent: SpatialKey => Extent
  )(
    calc: (Raster[Tile], Option[GridBounds[Int]]) => Tile
  ): Seq[(K, Tile)] =
    applyOnRaster(
      collection.bufferTiles(neighborhood.extent, layerBounds),
      neighborhood,
      keyToExtent
    )(calc)

  def applyOnRaster[K: SpatialComponent: GetComponent[*, SpatialKey]](
    rasterCollection: TileLayerCollection[K],
    neighborhood: Neighborhood
  )(
    calc: (Raster[Tile], Option[GridBounds[Int]]) => Tile
  ): TileLayerCollection[K] =
    rasterCollection.withContext { collection =>
      applyOnRaster(
        collection,
        neighborhood,
        rasterCollection.metadata.tileBounds,
        (key: SpatialKey) => rasterCollection.metadata.mapTransform.keyToExtent(key)
      )(calc)
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

  def focalWithExtents(n: Neighborhood)(calc: (Raster[Tile], Option[GridBounds[Int]], CellSize) => Tile): TileLayerCollection[K] = {
    val cellSize = self.metadata.layout.cellSize

    CollectionFocalOperation.applyOnRaster(self, n){ (raster, bounds) =>
      calc(raster, bounds, cellSize)
    }
  }
}
