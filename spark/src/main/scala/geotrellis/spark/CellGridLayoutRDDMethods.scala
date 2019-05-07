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

package geotrellis.spark

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.util._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class CellGridLayoutRDDMethods[K: SpatialComponent: ClassTag, V <: CellGrid[Int], M: GetComponent[?, LayoutDefinition]]
    extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def asRasters(): RDD[(K, Raster[V])] = {
    val layout = self.metadata.getComponent[LayoutDefinition]

    self.mapPartitions({ part =>
      part.map { case (key, tile) =>
        (key, Raster(tile, key.getComponent[SpatialKey].extent(layout)))
      }
    }, true)
  }
}
