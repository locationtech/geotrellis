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

package geotrellis.spark.resample

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.rdd._

object Implicits extends Implicits

trait Implicits {
  implicit class withLayerRDDZoomResampleMethods[
    K: SpatialComponent,
    V <: CellGrid[Int]: (? => TileResampleMethods[V])
  ](self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends LayerRDDZoomResampleMethods[K, V](self)
}
