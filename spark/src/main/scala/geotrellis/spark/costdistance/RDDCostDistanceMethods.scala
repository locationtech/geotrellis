/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.costdistance

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.raster.{DoubleArrayTile, Tile}
import geotrellis.tiling.SpatialKey
import geotrellis.spark._
import geotrellis.util.MethodExtensions
import geotrellis.vector.Geometry
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext


abstract class RDDCostDistanceMethods[K: (? => SpatialKey), V: (? => Tile)]
    extends MethodExtensions[RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {

  def costdistance(
    geometries: Seq[Geometry]
  )(implicit sc: SparkContext): RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]] =
    IterativeCostDistance(self, geometries)

  def costdistance(
    geometries: Seq[Geometry],
    maxCost: Double
  )(implicit sc: SparkContext): RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]] =
    IterativeCostDistance(self, geometries, maxCost)

}
