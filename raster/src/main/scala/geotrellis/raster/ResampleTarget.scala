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

package geotrellis.raster

import geotrellis.raster.reproject.Reproject
import spire.math.Integral

/**
 *  A strategy to use when constructing a grid that will be used during resampling
 * 
 *  Various tradeoffs exist when attempting to resize an image. Subclasses of ResampleTarget
 *  encode these various tradeoffs in terms of the proportions/alignment desired at the end
 *  of resampling. E.g. [[TargetCellSize]] tells any resampling function to resample in such 
 *  a way as to approximately match the specified [[CellSize]] in any resulting operations.
 */
sealed trait ResampleTarget {
  /**
   * Provided a gridextent, construct a new [[GridExtent]] that satisfies target constraint(s)
   * @param source a grid extent to be reshaped; call by name as it does not need to be called in all cases
   */
  def apply[N: Integral](source: => GridExtent[N]): GridExtent[N]
}

/** Resample, aiming for a specific number of cell columns/rows */
case class TargetDimensions(cols: Long, rows: Long) extends ResampleTarget {
  def apply[N: Integral](source: => GridExtent[N]): GridExtent[N] =
    new GridExtent(source.extent, cols, rows).toGridType[N]
}

/**
 * Snap to a target grid - useful prior to comparison between rasters
 * as a means of ensuring clear correspondence between underlying cell values
 */
case class TargetAlignment(grid: GridExtent[_]) extends ResampleTarget {
  def apply[N: Integral](source: => GridExtent[N]): GridExtent[N] =
    grid.createAlignedGridExtent(source.extent).toGridType[N]
}

/** Resample, sampling values into a user-supplied [[GridExtent]] */
case class TargetRegion(region: GridExtent[_]) extends ResampleTarget {
  def apply[N: Integral](source: => GridExtent[N]): GridExtent[N] =
    region.toGridType[N]
}

/**
 * Resample, aiming for a grid which has the provided [[CellSize]]
 *
 * @note Targetting a specific size for each cell in the grid has consequences for the
 * [[Extent]] because e.g. an extent's width *must* be evenly divisible by the width of
 * the cells within it. Consequently, we have two options: either modify the resolution
 * to accomodate the output extent or modify the overall extent to preserve the desired
 * output resolution. Fine grained constraints on both resolution and extent will currently
 * need to be managed manually.
 */
case class TargetCellSize(cellSize: CellSize) extends ResampleTarget {
  def apply[N: Integral](source: => GridExtent[N]): GridExtent[N] =
    source.withResolution(cellSize)
}

/**
 * The default resample target is used during reprojection.
 * A heuristic will be used to determine the best target proportions
 *
 * @note If used as target of resample operation it acts as identity operation.
 */
case object DefaultTarget extends ResampleTarget {
  def apply[N: Integral](source: => GridExtent[N]): GridExtent[N] = source
}


object ResampleTarget {
  /** Used when reprojecting to original RasterSource CRS, pick-out the grid */
  private[geotrellis] def fromReprojectOptions(options: Reproject.Options): ResampleTarget ={
    if (options.targetRasterExtent.isDefined) {
      TargetRegion(options.targetRasterExtent.get.toGridType[Long])
    } else if (options.parentGridExtent.isDefined) {
      TargetAlignment(options.parentGridExtent.get)
    } else if (options.targetCellSize.isDefined) {
      ??? // TODO: convert from CellSize to Column count based on ... something
    } else {
      DefaultTarget
    }
  }

  /** Used when resampling on already reprojected RasterSource */
  private[geotrellis] def toReprojectOptions(
    current: GridExtent[Long],
    resampleTarget: ResampleTarget,
    resampleMethod: ResampleMethod
  ): Reproject.Options = {
    resampleTarget match {
      case TargetDimensions(cols, rows) =>
        val updated = current.withDimensions(cols.toLong, rows.toLong).toGridType[Int]
        Reproject.Options(method = resampleMethod, targetRasterExtent = Some(updated.toRasterExtent))

      case TargetAlignment(grid) =>
        Reproject.Options(method = resampleMethod, parentGridExtent = Some(grid.toGridType[Long]))

      case TargetRegion(region) =>
        Reproject.Options(method = resampleMethod, targetRasterExtent = Some(region.toGridType[Int].toRasterExtent))

      case TargetCellSize(cellSize) =>
        Reproject.Options(method = resampleMethod, targetCellSize = Some(cellSize))

      case DefaultTarget =>
        Reproject.Options.DEFAULT.copy(method = resampleMethod)
    }
  }
}