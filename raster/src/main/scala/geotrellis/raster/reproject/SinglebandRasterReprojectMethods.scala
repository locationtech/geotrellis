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
import geotrellis.raster.rasterize._
import geotrellis.proj4._
import geotrellis.vector.Polygon

import spire.syntax.cfor._
import spire.math.Integral

trait SinglebandRasterReprojectMethods extends RasterReprojectMethods[SinglebandRaster] {

  def reproject(
    transform: Transform,
    inverseTransform: Transform,
    resampleTarget: ResampleTarget
  ): SinglebandRaster =
    reproject(transform, inverseTransform, resampleTarget, NearestNeighbor, 0.0)

  def reproject(
    transform: Transform,
    inverseTransform: Transform,
    resampleTarget: ResampleTarget,
    resampleMethod: ResampleMethod = NearestNeighbor,
    errorThreshold: Double = 0.0
  ): SinglebandRaster = {
    val Raster(tile, extent) = self

    val targetRasterExtent = ReprojectRasterExtent(self.rasterExtent, transform, resampleTarget)
    //val targetRasterExtent: RasterExtent = resampleTarget(self.rasterExtent.toGridType[N]).toRasterExtent

    val RasterExtent(newExtent, newCellWidth, newCellHeight, newCols, newRows) = targetRasterExtent

    val newTile = ArrayTile.empty(tile.cellType, newCols, newRows)

    val rowTransform: RowTransform =
      if (errorThreshold != 0.0)
        RowTransform.approximate(inverseTransform, errorThreshold)
      else
        RowTransform.exact(inverseTransform)

    // The map coordinates of the destination raster
    val (topLeftX, topLeftY) = targetRasterExtent.gridToMap(0, 0)
    val destX = Array.ofDim[Double](newCols)
    var currX = topLeftX
    cfor(0)(_ < newCols, _ + 1) { i =>
      destX(i) = currX
      currX += newCellWidth
    }

    val destY = Array.ofDim[Double](newCols).fill(topLeftY)

    // The map coordinates of the source raster, transformed from the
    // destination map coordinates on each row iteration
    val srcX = Array.ofDim[Double](newCols)
    val srcY = Array.ofDim[Double](newCols)

    val resampler = Resample(resampleMethod, tile, extent, CellSize(newCellWidth, newCellHeight))

    if(tile.cellType.isFloatingPoint) {
      cfor(0)(_ < newRows, _ + 1) { row =>
        // Reproject this whole row.
        rowTransform(destX, destY, srcX, srcY)
        cfor(0)(_ < newCols, _ + 1) { col =>
          val x = srcX(col)
          val y = srcY(col)
          val v = resampler.resampleDouble(x, y)
          newTile.setDouble(col, row, v)

          // Add row height for next iteration
          destY(col) -= newCellHeight
        }
      }
    } else {
      cfor(0)(_ < newRows, _ + 1) { row =>
        // Reproject this whole row.
        rowTransform(destX, destY, srcX, srcY)
        cfor(0)(_ < newCols, _ + 1) { col =>
          val x = srcX(col)
          val y = srcY(col)
          val v = resampler.resample(x, y)
          newTile.set(col, row, v)

          // Add row height for next iteration
          destY(col) -= newCellHeight
        }
      }
    }

    Raster(newTile, newExtent)
  }
}
