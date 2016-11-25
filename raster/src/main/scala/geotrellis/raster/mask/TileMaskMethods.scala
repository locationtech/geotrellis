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
import geotrellis.raster.mapalgebra.local.{Mask, InverseMask}
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Geometry, Extent}


/**
  * A trait containing extension methods related to masking of
  * tiles.
  */
trait TileMaskMethods[T] extends MethodExtensions[T] {

  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster *are not* set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be empty -- all values
    * set to NODATA.
    */
  def localMask(r: T, readMask: Int, writeMask: Int): T

  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster is set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be identical to the first
    * raster.
    */
  def localInverseMask(r: T, readMask: Int, writeMask: Int): T

  /**
    * Masks this tile by the given Geometry. Do not include polygon
    * exteriors.
    */
  def mask(ext: Extent, geom: Geometry): T =
    mask(ext, Seq(geom), Options.DEFAULT)

  /**
    * Masks this tile by the given Geometry.
    */
  def mask(ext: Extent, geom: Geometry, options: Options): T =
    mask(ext, Seq(geom), options)

  /**
    * Masks this tile by the given Geometry. Do not include polygon
    * exteriors.
    */
  def mask(ext: Extent, geoms: Traversable[Geometry]): T =
    mask(ext, geoms, Options.DEFAULT)

  /**
    * Masks this tile by the given Geometry.
    */
  def mask(ext: Extent, geoms: Traversable[Geometry], options: Options): T
}
