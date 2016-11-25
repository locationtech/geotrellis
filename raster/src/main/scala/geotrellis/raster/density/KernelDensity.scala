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

package geotrellis.raster.density

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.mapalgebra.focal.Kernel

/**
  * Object containing functions pertaining to kernel density
  * estimation.
  */
object KernelDensity {
  /**
    * Computes a Density raster based on the Kernel and set of points
    * provided.  Defaults to IntConstantNoDataCellType.
    */
  def apply(points: Traversable[PointFeature[Int]],
            kernel: Kernel,
            rasterExtent: RasterExtent): Tile =
    apply(points, kernel, rasterExtent, IntConstantNoDataCellType)

  /**
    * Computes a Density raster based on the Kernel and set of int
    * point features provided.
    *
    * @param      points           Sequence of point features who's values will be used to
    *                              compute the density.
    * @param      kernel           [[geotrellis.raster.mapalgebra.focal.Kernel]] to be used in the computation.
    * @param      rasterExtent     Raster extent of the resulting raster.
    * @param      cellType         CellType of the resulting tile.
    *
    */
  def apply(points: Traversable[PointFeature[Int]],
            kernel: Kernel,
            rasterExtent: RasterExtent,
            cellType: CellType): Tile = {
    val stamper = KernelStamper(cellType, rasterExtent.cols, rasterExtent.rows, kernel)

    for(point <- points) {
      val (col, row) = rasterExtent.mapToGrid(point.geom)
      if(cellType.isFloatingPoint) {
        stamper.stampKernelDouble(col, row, point.data.toDouble)
      } else {
        stamper.stampKernel(col, row, point.data)
      }
    }

    stamper.result
  }

  /**
    * Computes a Density raster based on the Kernel and set of points provided.
    * Defaults to DoubleConstantNoDataCellType.
    */
  def apply(points: Traversable[PointFeature[Double]],
            kernel: Kernel,
            rasterExtent: RasterExtent)(implicit d: DummyImplicit): Tile =
    apply(points, kernel, rasterExtent, DoubleConstantNoDataCellType)

  /**
    * Computes a Density raster based on the Kernel and set of double
    * point features provided.
    *
    * @param      points           Sequence of point features who's values will be used to
    *                              compute the density.
    * @param      kernel           [[geotrellis.raster.mapalgebra.focal.Kernel]] to be used in the computation.
    * @param      rasterExtent     Raster extent of the resulting raster.
    * @param      cellType         CellType of the resulting tile.
    *
    */
  def apply(points: Traversable[PointFeature[Double]],
            kernel: Kernel,
            rasterExtent: RasterExtent,
            cellType: CellType)(implicit d: DummyImplicit): Tile = {
    val stamper = KernelStamper(cellType, rasterExtent.cols, rasterExtent.rows, kernel)

    for(point <- points) {
      val (col, row) = rasterExtent.mapToGrid(point.geom)
      stamper.stampKernelDouble(col, row, point.data)
    }

    stamper.result
  }

}
