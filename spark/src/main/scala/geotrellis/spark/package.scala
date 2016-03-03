/*
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.util._

import geotrellis.spark.tiling._
import geotrellis.spark.ingest._

import org.apache.spark.Partitioner
import org.apache.spark.rdd._

import spire.syntax.cfor._

import monocle.{Lens, PLens}
import monocle.syntax._

import scala.reflect.ClassTag
import scalaz.Functor

package object spark
    extends buffer.Implicits
    with mask.Implicits
    with merge.Implicits
    with reproject.Implicits
    with tiling.Implicits
    with stitch.Implicits
    with mapalgebra.Implicits
    with mapalgebra.local.Implicits
    with mapalgebra.local.temporal.Implicits
    with mapalgebra.focal.Implicits
    with mapalgebra.zonal.Implicits
    with summary.polygonal.Implicits
    with summary.Implicits
    with mapalgebra.focal.hillshade.Implicits
    with partitioner.Implicits
    with Serializable // required for java serialization, even though it's mixed in
{
  type RasterRDD[K] = RDD[(K, Tile)] with Metadata[RasterMetaData[K]]

  object RasterRDD {
    def apply[K](rdd: RDD[(K, Tile)], metadata: RasterMetaData[K]): RasterRDD[K] =
      new ContextRDD(rdd, metadata)
  }

  type MultiBandRasterRDD[K] = RDD[(K, MultiBandTile)] with Metadata[RasterMetaData[K]]
  object MultiBandRasterRDD {
    def apply[K](rdd: RDD[(K, MultiBandTile)], metadata: RasterMetaData[K]): MultiBandRasterRDD[K] =
      new ContextRDD(rdd, metadata)
  }

  type Component[T, C] = PLens[T, T, C, C]

  object Component {
    def apply[T, C](get: T => C, set: (T, C) => T): Component[T, C] =
      PLens[T, T, C, C](get)(c => t => set(t, c))
  }

  implicit def identityComponent[T]: Component[T, T] =
    Component(v => v, (_, v) => v)

  /** Describes a getter and setter for an object that has
    * an implicitly defined lens into a component of that object
    * with a specific type.
    */
  implicit class ComponentMethods[T](val self: T) extends MethodExtensions[T] {
    def getComponent[C]()(implicit component: Component[T, C]): C =
      component.get(self)

    def setComponent[C](value: C)(implicit component: Component[T, C]): T =
      component.set(value)(self)
  }

  type SpatialComponent[K] = Component[K, SpatialKey]
  type TemporalComponent[K] = Component[K, TemporalKey]

  // implicit class SpatialComponentWrapper[K: SpatialComponent](key: K) {
  //   val _spatialComponent = implicitly[SpatialComponent[K]]

  //   def spatialComponent: SpatialKey = _spatialComponent.lens.get(key)

  //   def updateSpatialComponent(spatialKey: SpatialKey): K =
  //     _spatialComponent.lens.set(spatialKey)(key)
  // }

  // implicit class TemporalCompenentWrapper[K: TemporalComponent](key: K) {
  //   val _temporalComponent = implicitly[TemporalComponent[K]]

  //   def temporalComponent: TemporalKey = _temporalComponent.lens.get(key)

  //   def updateTemporalComponent(temporalKey: TemporalKey): K =
  //     _temporalComponent.lens.set(temporalKey)(key)
  // }

  // implicit class withBoundsComponentMethods[K, M: Component[?, Bounds[K]]](val self: M) extends MethodExtensions[M] {
  //   def getBounds: Bounds[K] = self.getComponent[Bounds[K]]
  // }

  type TileBounds = GridBounds

  /** Auto wrap a partitioner when something is requestion an Option[Partitioner];
    * useful for Options that take an Option[Partitioner]
    */
  implicit def partitionerToOption(partitioner: Partitioner): Option[Partitioner] =
    Some(partitioner)

  implicit class WithContextWrapper[K, V, M](val rdd: RDD[(K, V)] with Metadata[M]) {
    def withContext[K2, V2](f: RDD[(K, V)] => RDD[(K2, V2)]) =
      new ContextRDD(f(rdd), rdd.metadata)

    def mapContext[M2](f: M => M2) =
      new ContextRDD(rdd, f(rdd.metadata))
  }

  implicit def tupleToRDDWithMetadata[K, V, M](tup: (RDD[(K, V)], M)): RDD[(K, V)] with Metadata[M] =
    ContextRDD(tup._1, tup._2)

  implicit class withContextRDDMethods[K: ClassTag, V: ClassTag, M](rdd: RDD[(K, V)] with Metadata[M])
    extends ContextRDDMethods[K, V, M](rdd)

  implicit class withRasterRDDMethods[K](val self: RasterRDD[K])(implicit val keyClassTag: ClassTag[K])
    extends RasterRDDMethods[K]

  implicit class withRasterRDDMaskMethods[K: SpatialComponent: ClassTag](val self: RasterRDD[K])
      extends mask.RasterRDDMaskMethods[K]

  implicit class withMultiBandRasterRDDMethods[K](val self: MultiBandRasterRDD[K])(implicit val keyClassTag: ClassTag[K])
    extends MultiBandRasterRDDMethods[K]

  implicit class withProjectedExtentRDDMethods[K: Component[?, ProjectedExtent], V <: CellGrid](val rdd: RDD[(K, V)]) {
    def toRasters: RDD[(K, Raster[V])] =
      rdd.mapPartitions({ partition =>
        partition.map { case (key, value) =>
          (key, Raster(value, key.getComponent[ProjectedExtent].extent))
        }
      }, preservesPartitioning = true)
  }

  /** Keeps with the convention while still using simple tups, nice */
  implicit class TileTuple[K](tup: (K, Tile)) {
    def id: K = tup._1
    def tile: Tile = tup._2
  }

  implicit class withProjectedExtentTemporalTilerKeyMethods[K: Component[?, ProjectedExtent]: Component[?, TemporalKey]](val self: K) extends TilerKeyMethods[K, SpaceTimeKey] {
    def extent = self.getComponent[ProjectedExtent].extent
    def translate(spatialKey: SpatialKey): SpaceTimeKey = SpaceTimeKey(spatialKey, self.getComponent[TemporalKey])
  }

  implicit class withProjectedExtentTilerKeyMethods[K: Component[?, ProjectedExtent]](val self: K) extends TilerKeyMethods[K, SpatialKey] {
    def extent = self.getComponent[ProjectedExtent].extent
    def translate(spatialKey: SpatialKey) = spatialKey
  }
}
