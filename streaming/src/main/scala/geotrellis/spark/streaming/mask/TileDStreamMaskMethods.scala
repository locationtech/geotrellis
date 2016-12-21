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

package geotrellis.spark.streaming.mask

import geotrellis.raster.mask._
import geotrellis.spark._
import geotrellis.spark.mask.Mask
import geotrellis.spark.streaming._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

import Mask.Options

abstract class TileDStreamMaskMethods[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[DStream[(K, V)] with Metadata[M]] {
  /** Masks this raster by the given Polygon. */
  def mask(geom: Polygon): DStream[(K, V)] with Metadata[M] = mask(Seq(geom), Options.DEFAULT)

  def mask(geom: Polygon, options: Options): DStream[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this raster by the given Polygons. */
  def mask(geoms: Traversable[Polygon]): DStream[(K, V)] with Metadata[M] = mask(geoms, Options.DEFAULT)

  def mask(geoms: Traversable[Polygon], options: Options): DStream[(K, V)] with Metadata[M] =
    self.transformWithContext(Mask(_, geoms, options))

  /** Masks this raster by the given MultiPolygon. */
  def mask(geom: MultiPolygon): DStream[(K, V)] with Metadata[M] = mask(geom, Options.DEFAULT)

  def mask(geom: MultiPolygon, options: Options): DStream[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this raster by the given MultiPolygons. */
  def mask(geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): DStream[(K, V)] with Metadata[M] =
    self.transformWithContext(Mask(_, geoms, options))

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent): DStream[(K, V)] with Metadata[M] =
    mask(ext, Options.DEFAULT)

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent, options: Options): DStream[(K, V)] with Metadata[M] =
    self.transformWithContext(Mask(_, ext, options))
}
