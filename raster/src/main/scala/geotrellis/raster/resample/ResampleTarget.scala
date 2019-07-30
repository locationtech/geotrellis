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
import geotrellis.raster.CellSize
import geotrellis.vector.Extent

import spire.math.Integral
import spire.implicits._

/** Represents a strategy for transforming [[GridExtent]]s */
sealed trait ResampleTarget[N] {
  /** Provided a gridextent, construct a new [[GridExtent]] that satisfies target constraint(s) */
  def apply(source: => GridExtent[N]): GridExtent[N]

  /** Provided an extent, construct a new [[GridExtent]] that satisfies target constraint(s) */
  def fromExtent(extent: => Extent): GridExtent[N]
}

/** Resize provided [[GridExtent]] to the ensure specified columns and rows */
case class TargetDimensions[N: Integral](cols: N, rows: N) extends ResampleTarget[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    new GridExtent(source.extent, cols, rows)
  def fromExtent(extent: => Extent): GridExtent[N] =
    new GridExtent(extent, cols, rows)
}

/** Snap provided [[GridExtent]] to the target grid */
case class TargetGrid[N: Integral](grid: GridExtent[Long]) extends ResampleTarget[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    grid.createAlignedGridExtent(source.extent).toGridType[N]
  def fromExtent(extent: => Extent): GridExtent[N] =
    grid.createAlignedGridExtent(extent).toGridType[N]
}

/** Transform provided [[GridExtent]] into the one provided during construction */
case class TargetGridExtent[N: Integral](gridExtent: GridExtent[N]) extends ResampleTarget[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    gridExtent
  def fromExtent(extent: => Extent): GridExtent[N] =
    gridExtent
}

/** Transform provided [[GridExtent]] to accomodate the constructor [[CellSize]] */
case class TargetCellSize[N: Integral](cellSize: CellSize) extends ResampleTarget[N] {
  def apply(source: => GridExtent[N]): GridExtent[N] =
    source.withResolution(cellSize)

  // TODO: Investigate this behavior and potentially use the more sophisticated strategy from ReprojectRasterExtent.
  // It seems odd that there's no modification of the extent if the desired CS doesn't neatly divide it up
  def fromExtent(extent: => Extent): GridExtent[N] = {
    val newCols = math.ceil((extent.xmax - extent.xmin) / cellSize.width)
    val newRows = math.ceil((extent.ymax - extent.ymin) / cellSize.height)
    new GridExtent(extent, cellSize.width, cellSize.height,
      cols = Integral[N].fromDouble(newCols),
      rows = Integral[N].fromDouble(newRows))
  }
}

