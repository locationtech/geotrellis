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

package geotrellis.spark.crop

import geotrellis.raster._
import geotrellis.raster.crop.TileCropMethods
import geotrellis.raster.crop.Crop.Options
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.spark.rdd._

object Crop {
  def apply[
    K: SpatialComponent,
    V <: CellGrid[Int]: (? => TileCropMethods[V]),
    M: Component[?, Bounds[K]]: GetComponent[?, Extent]: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], extent: Extent, options: Options): RDD[(K, V)] with Metadata[M] =
    rdd.metadata.getComponent[Extent].intersection(extent) match {
      case Some(intersectionExtent) =>
        rdd.metadata.getComponent[Bounds[K]] match {
          case kb @ KeyBounds(minKey, maxKey) =>
            val mapTransform = rdd.metadata.getComponent[LayoutDefinition].mapTransform

            val croppedRdd =
              rdd
                .mapPartitions({ partition =>
                  partition.flatMap({ case (key, tile) =>
                    val srcExtent: Extent = mapTransform.keyToExtent(key.getComponent[SpatialKey])

                    if (extent.contains(srcExtent)) {
                      Some((key, tile))
                    } else if (extent.interiorIntersects(srcExtent)) {
                      val newTile = tile.crop(srcExtent, extent.intersection(srcExtent).get, options)
                      Some((key, newTile))
                    } else {
                      None
                    }
                  })
                }, preservesPartitioning = true)

            val GridBounds(minCol, minRow, maxCol, maxRow) =
              mapTransform(intersectionExtent)

            val newKeyBounds =
              KeyBounds(
                minKey.setComponent(SpatialKey(minCol, minRow)),
                maxKey.setComponent(SpatialKey(maxCol, maxRow))
              )

            val md = rdd.metadata.setComponent[Bounds[K]](newKeyBounds)

            ContextRDD(croppedRdd, md)
          case EmptyBounds =>
            rdd
        }
      case None =>
        ContextRDD(rdd.sparkContext.parallelize(Seq()), rdd.metadata.setComponent[Bounds[K]](EmptyBounds))
    }
}
