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

package geotrellis.spark.mask

import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.mask._
import geotrellis.layers.Metadata
import geotrellis.layers.mask._
import geotrellis.layers.mask.Mask.Options
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withTileRDDMaskMethods[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](val self: RDD[(K, V)] with Metadata[M]) extends TileRDDMaskMethods[K, V, M]
}
