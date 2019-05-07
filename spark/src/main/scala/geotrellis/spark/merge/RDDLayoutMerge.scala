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

package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object RDDLayoutMerge {
  /** Merges an RDD with metadata that contains a layout definition into another. */
  def merge[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: (? => LayoutDefinition)
  ](left: RDD[(K, V)] with Metadata[M], right: RDD[(K, V)] with Metadata[M]) = {
    val thisLayout: LayoutDefinition = left.metadata
    val thatLayout: LayoutDefinition = right.metadata

    val cutRdd =
      right
        .flatMap { case (k: K, tile: V) =>
          val extent: Extent = k.getComponent[SpatialKey].extent(thatLayout)

          thisLayout.mapTransform(extent)
            .coordsIter
            .map { case (col, row) =>
              val outKey = k.setComponent(SpatialKey(col, row))
              val newTile = tile.prototype(thisLayout.tileCols, thisLayout.tileRows)
              val merged = newTile.merge(outKey.getComponent[SpatialKey].extent(thisLayout), extent, tile)
              (outKey, merged)
            }
        }


    left.withContext { rdd => rdd.merge(cutRdd) }
  }
}
