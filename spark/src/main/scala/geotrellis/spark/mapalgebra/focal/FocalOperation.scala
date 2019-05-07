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

import geotrellis.tiling.{SpatialComponent, TileBounds}
import geotrellis.spark._
import geotrellis.spark.buffer._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.util.MethodExtensions

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
}
