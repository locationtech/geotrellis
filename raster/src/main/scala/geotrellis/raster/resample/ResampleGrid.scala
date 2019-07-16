/*
 * Copyright 2019 Azavea
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

import geotrellis.raster.{RasterExtent, GridExtent, GridBounds}
import geotrellis.raster.reproject.Reproject
import geotrellis.raster.CellSize

import spire.math.Integral
import spire.implicits._

sealed trait ResampleGrid[N] {
  // this is a by name parameter, as we don't need to call the source in all ResampleGrid types
  def apply(source: => GridExtent[N]): GridExtent[N]
}

case class TargetDimensions[N: Integral](cols: N, rows: N) extends ResampleGrid[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    new GridExtent(source.extent, cols, rows)
}

case class TargetGrid[N: Integral](grid: GridExtent[Long]) extends ResampleGrid[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    grid.createAlignedGridExtent(source.extent).toGridType[N]
}

case class TargetRegion[N: Integral](region: GridExtent[N]) extends ResampleGrid[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    region
}

case class TargetBounds[N: Integral](bounds: GridBounds[N]) extends ResampleGrid[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    source.createAlignedGridExtent(source.extentFor(bounds, clamp=false))
}

case class TargetCellSize[N: Integral](cellSize: CellSize) extends ResampleGrid[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    source.withResolution(cellSize)
}

case object IdentityResampleGrid extends ResampleGrid[Long] {
  def apply(source: => GridExtent[Long]): GridExtent[Long] =
    source
}


object ResampleGrid {
  def targetRegion[N: Integral](region: GridExtent[N]): ResampleGrid[N] =
    TargetRegion(region)

  def targetRegion[N: Integral](region: RasterExtent): ResampleGrid[N] =
    targetRegion(region.toGridType[N])

  def targetCellSize[N: Integral](cellSize: CellSize): ResampleGrid[N] =
    TargetCellSize(cellSize)

}

