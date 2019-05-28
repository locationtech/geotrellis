/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.tiling.{SpatialKey, SpatialComponent}
import geotrellis.spark._
import geotrellis.layers.index.KeyIndex
import geotrellis.util._

import org.apache.spark.rdd.RDD

import java.net.URI

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits extends vrt.Implicits {
  implicit class withCOGLayerWriteMethods[K: SpatialComponent: ClassTag, V <: CellGrid[Int]: ClassTag](val self: RDD[(K, GeoTiff[V])]) extends MethodExtensions[RDD[(K, GeoTiff[V])]] {
    def write(keyIndex: KeyIndex[K], uri: URI): Unit =
      COGLayer.write[K, V](self)(keyIndex, uri)
  }
}
