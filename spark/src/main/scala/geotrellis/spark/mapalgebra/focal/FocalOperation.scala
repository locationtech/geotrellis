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

package geotrellis.spark.mapalgebra.focal

import geotrellis.layer.{SpatialComponent, TileBounds, SpatialKey}
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.buffer.BufferedTile
import geotrellis.raster.mapalgebra.focal._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner

import scala.reflect.ClassTag

object FocalOperation {
  private def mapOverBufferedTiles[K: SpatialComponent: ClassTag](bufferedTiles: RDD[(K, BufferedTile[Tile])], neighborhood: Neighborhood)
      (calc: (Tile, Option[GridBounds[Int]]) => Tile): RDD[(K, Tile)] =
    bufferedTiles
      .mapValues { case BufferedTile(tile, gridBounds) => calc(tile, Some(gridBounds)) }

  def apply[K: SpatialComponent: ClassTag](
    rdd: RDD[(K, Tile)],
    neighborhood: Neighborhood,
    partitioner: Option[Partitioner])
    (calc: (Tile, Option[GridBounds[Int]]) => Tile)(implicit d: DummyImplicit): RDD[(K, Tile)] =
      mapOverBufferedTiles(rdd.bufferTiles(neighborhood.extent, partitioner), neighborhood)(calc)

  def apply[K: SpatialComponent: ClassTag](
    rdd: RDD[(K, Tile)],
    neighborhood: Neighborhood,
    layerBounds: TileBounds,
    partitioner: Option[Partitioner])
    (calc: (Tile, Option[GridBounds[Int]]) => Tile): RDD[(K, Tile)] =
      mapOverBufferedTiles(rdd.bufferTiles(neighborhood.extent, layerBounds, partitioner), neighborhood)(calc)

  def apply[K: SpatialComponent: ClassTag](rasterRDD: TileLayerRDD[K], neighborhood: Neighborhood, partitioner: Option[Partitioner])
      (calc: (Tile, Option[GridBounds[Int]]) => Tile): TileLayerRDD[K] =
    rasterRDD.withContext { rdd =>
      apply(rdd, neighborhood, rasterRDD.metadata.tileBounds, partitioner)(calc)
    }

  private def applyOnRaster[K: SpatialComponent: ClassTag: GetComponent[*, SpatialKey]]
    (bufferedTiles: RDD[(K, BufferedTile[Tile])], neighborhood: Neighborhood, keyToExtent: SpatialKey => Extent)
    (calc: (Raster[Tile], Option[GridBounds[Int]]) => Tile): RDD[(K, Tile)] =
      bufferedTiles
        .mapPartitions({ case partition =>
          partition.map { case (k, BufferedTile(tile, gridBounds)) =>
            val spatialKey = k.getComponent[SpatialKey]

            k -> calc(Raster(tile, keyToExtent(spatialKey)), Some(gridBounds))
          }
        }, preservesPartitioning = true)

  def applyOnRaster[K: SpatialComponent: ClassTag: GetComponent[*, SpatialKey]](
    rdd: RDD[(K, Tile)],
    neighborhood: Neighborhood,
    layerBounds: TileBounds,
    partitioner: Option[Partitioner],
    keyToExtent: SpatialKey => Extent
  )(calc: (Raster[Tile], Option[GridBounds[Int]]) => Tile): RDD[(K, Tile)] =
      applyOnRaster(rdd.bufferTiles(neighborhood.extent, layerBounds, partitioner), neighborhood, keyToExtent)(calc)

  def applyOnRaster[
    K: SpatialComponent: ClassTag: GetComponent[*, SpatialKey]
  ](rasterRDD: TileLayerRDD[K], neighborhood: Neighborhood, partitioner: Option[Partitioner])
  (calc: (Raster[Tile], Option[GridBounds[Int]]) => Tile): TileLayerRDD[K] =
    rasterRDD.withContext { rdd =>
      applyOnRaster(
        rdd,
        neighborhood,
        rasterRDD.metadata.tileBounds,
        partitioner,
        (key: SpatialKey) => rasterRDD.metadata.mapTransform.keyToExtent(key)
      )(calc)
    }
}

abstract class FocalOperation[K: SpatialComponent: ClassTag] extends MethodExtensions[TileLayerRDD[K]] {

  def focal(n: Neighborhood, partitioner: Option[Partitioner])
      (calc: (Tile, Option[GridBounds[Int]]) => Tile): TileLayerRDD[K] =
        FocalOperation(self, n, partitioner)(calc)

  def focalWithCellSize(n: Neighborhood, partitioner: Option[Partitioner])
      (calc: (Tile, Option[GridBounds[Int]], CellSize) => Tile): TileLayerRDD[K] = {
    val cellSize = self.metadata.layout.cellSize
    FocalOperation(self, n, partitioner){ (tile, bounds) => calc(tile, bounds, cellSize) }
  }

  def focalWithExtents(n: Neighborhood, partitioner: Option[Partitioner])
      (calc: (Raster[Tile], Option[GridBounds[Int]], CellSize) => Tile): TileLayerRDD[K] = {
    val cellSize = self.metadata.layout.cellSize

    FocalOperation.applyOnRaster(self, n, partitioner){ (raster, bounds) => calc(raster, bounds, cellSize) }
  }
}
