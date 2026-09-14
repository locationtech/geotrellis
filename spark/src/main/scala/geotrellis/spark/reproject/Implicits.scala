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

import geotrellis.raster.*
import geotrellis.raster.crop.*
import geotrellis.raster.merge.*
import geotrellis.raster.prototype.*
import geotrellis.raster.reproject.*
import geotrellis.raster.stitch.*
import geotrellis.layer.*
import geotrellis.vector.*
import geotrellis.util.*
import org.apache.spark.rdd.*

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withProjectedExtentReprojectMethods[K: Component[_, ProjectedExtent], V <: CellGrid[Int]: _ => TileReprojectMethods[V]](self: RDD[(K, V)])
      extends ProjectedExtentComponentReprojectMethods[K, V](self) { }

  implicit class withTileRDDReprojectMethods[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: _ => TileReprojectMethods[V]: _ => CropMethods[V]: _ => TileMergeMethods[V]: _ => TilePrototypeMethods[V]
  ](self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends TileRDDReprojectMethods[K, V](self)
}
