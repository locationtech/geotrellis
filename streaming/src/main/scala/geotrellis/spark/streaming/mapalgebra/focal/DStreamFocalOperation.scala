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

package geotrellis.spark.streaming.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.spark._
import geotrellis.spark.buffer._
import geotrellis.spark.streaming._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

object DStreamFocalOperation {
  private def mapOverBufferedTiles[K: SpatialComponent: ClassTag](bufferedTiles: DStream[(K, BufferedTile[Tile])], neighborhood: Neighborhood)
      (calc: (Tile, Option[GridBounds]) => Tile): DStream[(K, Tile)] =
    bufferedTiles
      .mapValues { case BufferedTile(tile, gridBounds) => calc(tile, Some(gridBounds)) }

  def apply[K: SpatialComponent: ClassTag](stream: DStream[(K, Tile)], neighborhood: Neighborhood)
      (calc: (Tile, Option[GridBounds]) => Tile)(implicit d: DummyImplicit): DStream[(K, Tile)] =
    mapOverBufferedTiles(stream.bufferTiles(neighborhood.extent), neighborhood)(calc)

  def apply[K: SpatialComponent: ClassTag](stream: DStream[(K, Tile)], neighborhood: Neighborhood, layerBounds: GridBounds)
      (calc: (Tile, Option[GridBounds]) => Tile): DStream[(K, Tile)] =
    mapOverBufferedTiles(stream.bufferTiles(neighborhood.extent, layerBounds), neighborhood)(calc)

  def apply[K: SpatialComponent: ClassTag](stream: TileLayerDStream[K], neighborhood: Neighborhood)
      (calc: (Tile, Option[GridBounds]) => Tile): TileLayerDStream[K] =
    stream.withContext { s =>
      apply(s, neighborhood, stream.metadata.gridBounds)(calc)
    }
}

abstract class DStreamFocalOperation[K: SpatialComponent: ClassTag] extends MethodExtensions[TileLayerDStream[K]] {

  def focal(n: Neighborhood)
      (calc: (Tile, Option[GridBounds]) => Tile): TileLayerDStream[K] =
    DStreamFocalOperation(self, n)(calc)

  def focalWithCellSize(n: Neighborhood)
      (calc: (Tile, Option[GridBounds], CellSize) => Tile): TileLayerDStream[K] = {
    val cellSize = self.metadata.layout.cellSize
    DStreamFocalOperation(self, n){ (tile, bounds) => calc(tile, bounds, cellSize) }
  }
}
