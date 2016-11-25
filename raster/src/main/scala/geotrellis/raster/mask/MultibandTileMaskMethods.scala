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
import geotrellis.vector.{Geometry, Extent}


/**
  * A trait containing extension methods related to masking of
  * [[MultibandTile]]s.
  */
trait MultibandTileMaskMethods extends TileMaskMethods[MultibandTile] {
  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster *are not* set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be empty -- all values
    * set to NODATA.
    */
  def localMask(r: MultibandTile, readMask: Int, writeMask: Int): MultibandTile =
    ArrayMultibandTile(
      self.bands
        .zip(r.bands)
        .map { case (t, m) => t.localMask(m, readMask, writeMask) }
    )

  /**
    * Generate a raster with the values from the first raster, but
    * only include cells in which the corresponding cell in the second
    * raster is set to the "readMask" value.
    *
    * For example, if *all* cells in the second raster are set to the
    * readMask value, the output raster will be identical to the first
    * raster.
    */
  def localInverseMask(r: MultibandTile, readMask: Int, writeMask: Int): MultibandTile =
    ArrayMultibandTile(
      self.bands
        .zip(r.bands)
        .map { case (t, m) => t.localInverseMask(m, readMask, writeMask) }
    )

  /**
    * Masks this tile by the given Geometry.
    */
  def mask(ext: Extent, geoms: Traversable[Geometry], options: Options): MultibandTile =
    ArrayMultibandTile(
      self.bands.map(_.mask(ext, geoms, options))
    )
}
