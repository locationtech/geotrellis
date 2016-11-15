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

package geotrellis.raster.mask

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Geometry, Extent}

/**
  * A trait containing extension methods related to masking of a
  * [[Raster]].
  */
trait RasterMaskMethods[T <: CellGrid] extends MethodExtensions[Raster[T]] {
  /**
    * Masks this raster by the given Geometry. Do not include polygon
    * exteriors.
    */
  def mask(geom: Geometry): Raster[T] =
    mask(Seq(geom), Options.DEFAULT)

  /**
    * Masks this raster by the given Geometry.
    */
  def mask(geom: Geometry, options: Options): Raster[T] =
    mask(Seq(geom), options)

  /**
    * Masks this raster by the given Geometry. Do not include polygon
    * exteriors.
    */
  def mask(geoms: Traversable[Geometry]): Raster[T] =
    mask(geoms, Options.DEFAULT)

  /**
    * Masks this raster by the given Geometry.
    */
  def mask(geoms: Traversable[Geometry], options: Options): Raster[T]
}
