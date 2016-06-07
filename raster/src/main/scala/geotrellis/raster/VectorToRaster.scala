/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import geotrellis.vector._
import geotrellis.raster.rasterize._
import geotrellis.raster.mapalgebra.focal.{Circle, Kernel, Square}
import spire.syntax.cfor._

/**
  * Object that holds various functions for vector-to-raster
  * computations.
  */
object VectorToRaster {

  /**
    * Computes a Density raster based on the Kernel and set of points provided.
    *
    * @param      points           Sequence of point features who's values will be used to
    *                              compute the density.
    * @param      kernel           [[Kernel]] to be used in the computation.
    * @param      rasterExtent     Raster extent of the resulting raster.
    * @note                        KernelDensity does not currently support Double raster data.
    *                              If you use a Raster with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
    *                              the data values will be rounded to integers.
    */
  def kernelDensity[D](points: Seq[PointFeature[D]],
                       kernel: Kernel,
                       rasterExtent: RasterExtent)
                      (implicit transform:D => Int): Tile =
    kernelDensity(points, transform, kernel, rasterExtent)

  /**
    * Computes a Density raster based on the Kernel and set of points provided.
    *
    * @param      points           Sequence of point features who's values will be used to
    *                              compute the density.
    * @param      transform        Function that transforms the point feature's data into
    *                              an Int value.
    * @param      kernel           [[Kernel]] to be used in the computation.
    * @param      rasterExtent     Raster extent of the resulting raster.
    * @note                        KernelDensity does not currently support Double raster data.
    *                              If you use a Raster with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
    *                              the data values will be rounded to integers.
    */
  def kernelDensity[D](points: Seq[PointFeature[D]],
                       transform: D => Int,
                       kernel: Kernel,
                       rasterExtent: RasterExtent): Tile = {
    val stamper = KernelStamper(IntConstantNoDataCellType, rasterExtent.cols, rasterExtent.rows, kernel)

    for(point <- points) {
      val col = rasterExtent.mapXToGrid(point.geom.x)
      val row = rasterExtent.mapYToGrid(point.geom.y)
      stamper.stampKernel(col, row, transform(point.data))
    }

    stamper.result
  }

  /**
    * Gives a raster that represents the number of occurring points per
    * cell.
    *
    * @param points        Sequence of points to be counted.
    * @param rasterExtent  RasterExtent of the resulting raster.
    */
  def countPoints(points: Seq[Point], rasterExtent: RasterExtent): Tile = {
    val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)
    val array = Array.ofDim[Int](cols * rows).fill(0)
    for(point <- points) {
      val x = point.x
      val y = point.y
      if(rasterExtent.extent.intersects(x,y)) {
        val index = rasterExtent.mapXToGrid(x) * cols + rasterExtent.mapYToGrid(y)
        array(index) = array(index) + 1
      }
    }
    IntArrayTile(array, cols, rows)
  }
}
