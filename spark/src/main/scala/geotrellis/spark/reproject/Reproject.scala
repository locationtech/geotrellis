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

package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.resample._

case class RasterReprojectOptions(
  method: ResampleMethod = ResampleMethods.NearestNeighbor,
  errorThreshold: Double = 0.125,
  parentGridExtent: Option[GridExtent[Long]] = None,
  targetRasterExtent: Option[RasterExtent] = None,
  targetCellSize: Option[CellSize] = None
)

object RasterReprojectOptions {
  def DEFAULT = RasterReprojectOptions()
}

object Reproject {
  case class Options(
    rasterReprojectOptions: RasterReprojectOptions = RasterReprojectOptions.DEFAULT,
    /** Attempts to match the total layer extent.
      * Warning: This should only be used on layers with smaller extents, and only
      * if you really need it to match what a reprojection would be on the parent
      * layer as one raster. Seams can happen on layers that use this that cover
      * too wide of an area.
      */
    matchLayerExtent: Boolean = false
  )

  object Options {
    def DEFAULT = Options()

    implicit def rasterReprojectOptionsToOptions(rro: RasterReprojectOptions): Options =
      Options(rasterReprojectOptions = rro)

   implicit def resampleMethodToOptions(method: ResampleMethod): Options =
      Options(rasterReprojectOptions = RasterReprojectOptions(method = method))
  }
}
