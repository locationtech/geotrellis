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

package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object CutTiles extends LazyLogging {
  def apply[
    K1: (? => TilerKeyMethods[K1, K2]),
    K2: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ] (
    rdd: RDD[(K1, V)],
    cellType: CellType,
    layoutDefinition: LayoutDefinition,
    resampleMethod: ResampleMethod = NearestNeighbor
  ): RDD[(K2, V)] = {
    logger.debug(s"CutTiles($rdd, $cellType, $resampleMethod)")
    val mapTransform = layoutDefinition.mapTransform
    val (tileCols, tileRows) = layoutDefinition.tileLayout.tileDimensions

    rdd
      .flatMap { tup =>
        val (inKey, tile) = tup
        val extent = inKey.extent
        logger.debug(s"Cutting $inKey of ${tile.dimensions} cells covering $extent")
        mapTransform(extent)
          .coordsIter
          .map  { spatialComponent =>
            val outKey = inKey.translate(spatialComponent)
            logger.debug(s"Merge $inKey into $outKey of (${tileCols}, ${tileRows}) cells")
            val newTile = tile.prototype(cellType, tileCols, tileRows)
            (outKey, newTile.merge(
               mapTransform.keyToExtent(outKey.getComponent[SpatialKey]),
               extent,
               tile,
               resampleMethod
             ))
          }
      }
  }
}
