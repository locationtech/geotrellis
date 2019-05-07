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

package geotrellis.spark.mapalgebra.local.temporal

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {

  implicit class withLocalTemporalTileRDDMethods[K: ClassTag: SpatialComponent: TemporalComponent](self: RDD[(K, Tile)])
      extends LocalTemporalTileRDDMethods[K](self)

  implicit class withLocalTemporalTileCollectionMethods[K: SpatialComponent: TemporalComponent](self: Seq[(K, Tile)])
    extends LocalTemporalTileCollectionMethods[K](self)

  implicit class TemporalWindow[K: ClassTag: SpatialComponent: TemporalComponent](val self: RDD[(K, Tile)]) extends MethodExtensions[RDD[(K, Tile)]] {

    import TemporalWindowHelper._

    def average: TemporalWindowState[K] = TemporalWindowState(self, Average)

    def minimum: TemporalWindowState[K] = TemporalWindowState(self, Minimum)

    def maximum: TemporalWindowState[K] = TemporalWindowState(self, Maximum)

    def variance: TemporalWindowState[K] = TemporalWindowState(self, Variance)
  }
}
