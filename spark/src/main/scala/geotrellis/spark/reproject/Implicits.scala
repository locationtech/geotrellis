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

package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.reproject._
import geotrellis.raster.stitch._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withProjectedExtentReprojectMethods[K: Component[?, ProjectedExtent], V <: CellGrid[Int]: (? => TileReprojectMethods[V])](self: RDD[(K, V)])
      extends ProjectedExtentComponentReprojectMethods[K, V](self) { }

  implicit class withTileRDDReprojectMethods[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends TileRDDReprojectMethods[K, V](self)
}
