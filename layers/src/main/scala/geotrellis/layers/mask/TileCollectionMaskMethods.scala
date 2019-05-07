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

package geotrellis.layers.mask

import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.raster.mask._
import geotrellis.layers.Metadata
import Mask.Options
import geotrellis.util._


abstract class TileCollectionMaskMethods[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[Seq[(K, V)] with Metadata[M]] {
  /** Masks this raster by the given Polygon. */
  def mask(geom: Polygon): Seq[(K, V)] with Metadata[M] = mask(Seq(geom), Options.DEFAULT)

  def mask(geom: Polygon, options: Options): Seq[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this raster by the given Polygons. */
  def mask(geoms: Traversable[Polygon]): Seq[(K, V)] with Metadata[M] = mask(geoms, Options.DEFAULT)

  def mask(geoms: Traversable[Polygon], options: Options): Seq[(K, V)] with Metadata[M] =
    Mask(self, geoms, options)

  /** Masks this raster by the given MultiPolygon. */
  def mask(geom: MultiPolygon): Seq[(K, V)] with Metadata[M] = mask(geom, Options.DEFAULT)

  def mask(geom: MultiPolygon, options: Options): Seq[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this raster by the given MultiPolygons. */
  def mask(geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): Seq[(K, V)] with Metadata[M] =
    Mask(self, geoms, options)

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent): Seq[(K, V)] with Metadata[M] =
    mask(ext, Options.DEFAULT)

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent, options: Options): Seq[(K, V)] with Metadata[M] =
    Mask(self, ext, options)
}
