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

package geotrellis.raster.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Geometry,Feature}


/**
  * A trait providing extension methods for invoking the rasterizer on
  * Feature objects.
  */
trait FeatureIntRasterizeMethods[+G <: Geometry] extends MethodExtensions[Feature[G, Int]] {

  /**
    * Fill in this feature's data value at each cell of given
    * [[RasterExtent]] that is covered by the present Feature.  The
    * result is a [[Raster]].
    */
  def rasterize(
    re: RasterExtent,
    ct: CellType = IntConstantNoDataCellType,
    options: Options = Options.DEFAULT
  ): Raster[Tile] =
    self.geom.rasterizeWithValue(re, self.data, ct, options)
}

/**
  * A trait providing extension methods for invoking the rasterizer on
  * Feature objects.
  */
trait FeatureDoubleRasterizeMethods[+G <: Geometry] extends MethodExtensions[Feature[G, Double]] {

  /**
    * Fill in this feature's data value at each cell of given
    * [[RasterExtent]] that is covered by the present Feature.  The
    * result is a [[Raster]].
    */
  def rasterize(
    re: RasterExtent,
    ct: CellType = DoubleConstantNoDataCellType,
    options: Options = Options.DEFAULT
  ): Raster[Tile] =
    self.geom.rasterizeWithValueDouble(re, self.data, ct, options)
}
