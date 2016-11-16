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

package geotrellis.geotools

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.geotools.coverage.grid._

trait MultibandRasterToGridCoverage2DMethods extends MethodExtensions[Raster[MultibandTile]] with ToGridCoverage2DMethods {
  def toGridCoverage2D(): GridCoverage2D =
    GridCoverage2DConverters.convertToGridCoverage2D(self)
}

trait MultibandProjectedRasterToGridCoverage2DMethods[T <: MultibandTile] extends MethodExtensions[ProjectedRaster[T]] with ToGridCoverage2DMethods {
  def toGridCoverage2D(): GridCoverage2D =
    GridCoverage2DConverters.convertToGridCoverage2D(self.raster, self.crs)
}
