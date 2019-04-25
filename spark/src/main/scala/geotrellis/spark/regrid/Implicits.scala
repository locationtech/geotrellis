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

package geotrellis.spark.regrid

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withRegridMethods[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: Stitcher: (? => CropMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](self: RDD[(K, V)] with Metadata[M]) extends RegridMethods[K, V, M](self)
}
