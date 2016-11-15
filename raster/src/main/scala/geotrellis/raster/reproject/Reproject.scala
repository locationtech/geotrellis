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

package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._

object Reproject {
  /** Reprojection options.
    *
    * @param      method               The resampling method that will be used in this reprojection.
    * @param      errorThreshold       Error threshold when using approximate row transformations in reprojection.
    *                                  This default comes from GDAL 1.11 code.
    * @param      parentGridExtent     An optional GridExtent that if set represents the target grid extent for some
    *                                  parent window, which reprojected extents will snap to. Use with caution.
    * @param      targetRasterExtent   The target raster extent to reproject to.
    * @param      targetCellSize       An optional cell size that if set will be used for for the projected raster.
    *                                  Use with caution.
    *
    */
  case class Options(
    method: ResampleMethod = NearestNeighbor,
    errorThreshold: Double = 0.125,
    parentGridExtent: Option[GridExtent] = None,
    targetRasterExtent: Option[RasterExtent] = None,
    targetCellSize: Option[CellSize] = None
  )

  object Options {
    def DEFAULT = Options()

    implicit def methodToOptions(method: ResampleMethod): Options =
      apply(method = method)
  }
}
