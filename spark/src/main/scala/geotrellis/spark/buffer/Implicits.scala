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

package geotrellis.spark.buffer

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.tiling.SpatialComponent
import geotrellis.spark._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withCollectNeighborsMethodsWrapper[
    K: SpatialComponent: ClassTag,
    V
  ](self: RDD[(K, V)]) extends CollectNeighborsMethods[K, V](self)

  implicit class withBufferTilesMethodsWrapper[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: ClassTag: (? => CropMethods[V])
  ](self: RDD[(K, V)]) extends BufferTilesMethods[K, V](self)

  implicit class withCollectionsBufferTilesMethodsWrapper[
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](self: Seq[(K, V)]) extends CollectionBufferTilesMethods[K, V](self)
}
