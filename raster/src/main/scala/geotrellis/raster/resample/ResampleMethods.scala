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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.util.MethodExtensions

import spire.syntax.cfor._


trait ResampleMethods[T <: CellGrid] extends MethodExtensions[T] {
  def resample(extent: Extent, targetExtent: RasterExtent, method: ResampleMethod): T

  def resample(source: Extent, target: RasterExtent): T =
    resample(source, target, ResampleMethod.DEFAULT)

  def resample(source: Extent, target: Extent): T =
    resample(source, target, ResampleMethod.DEFAULT)

  def resample(extent: Extent, targetExtent: Extent, method: ResampleMethod): T =
    resample(extent, RasterExtent(extent, self.cols, self.rows).createAlignedRasterExtent(targetExtent), method)

  def resample(extent: Extent, targetCols: Int, targetRows: Int): T =
    resample(extent, targetCols, targetRows, ResampleMethod.DEFAULT)

  def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): T =
    resample(extent, RasterExtent(extent, targetCols, targetRows), method)

  /** Only changes the resolution */
  def resample(targetCols: Int, targetRows: Int): T =
    resample(Extent(0.0, 0.0, 1.0, 1.0), targetCols, targetRows)
}
