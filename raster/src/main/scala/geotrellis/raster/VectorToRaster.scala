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

import spire.syntax.cfor._

object VectorToRaster { 

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
    *
    * @note                        KernelDensity does not currently support Double raster data.
    *                              If you use a Raster with a Double CellType (TypeFloat, TypeDouble)
    *                              the data values will be rounded to integers.
    */
  def kernelDensity[D](points: Seq[PointFeature[D]],
                       transform: D => Int, 
                       kernel: Kernel, 
                       rasterExtent: RasterExtent): Tile = {
    val convolver = new Convolver(rasterExtent.cols, rasterExtent.rows, kernel)
    
    for(point <- points) {
      val col = rasterExtent.mapXToGrid(point.geom.x)
      val row = rasterExtent.mapYToGrid(point.geom.y)
      convolver.stampKernel(col, row, transform(point.data))
    }

    convolver.result
  }

  def idwInterpolate(points: Seq[PointFeature[Int]], re: RasterExtent): Tile =
    idwInterpolate(points, re, None)

  def idwInterpolate(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Int): Tile =
    idwInterpolate(points, re, Some(radius))

  def idwInterpolate(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int]): Tile = {
    val cols = re.cols
    val rows = re.rows
    val tile = ArrayTile.empty(TypeInt, cols, rows)
    if(points.isEmpty) {
      tile
    } else {
      val r = radius match {
        case Some(r: Int) =>
          val rr = r*r
          val index: SpatialIndex[PointFeature[Int]] = SpatialIndex(points)(p => (p.geom.x, p.geom.y))

          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val destX = re.gridColToMap(col)
              val destY = re.gridRowToMap(row)
              val pts = index.pointsInExtent(Extent(destX - r, destY - r, destX + r, destY + r))
              println(pts.size)
              if (pts.isEmpty) {
                tile.set(col, row, NODATA)
              } else {
                var s = 0.0
                var c = 0
                var ws = 0.0
                val length = pts.size

                cfor(0)(_ < length, _ + 1) { i => 
                  val point = pts(i)
                  val dX = (destX - point.geom.x)
                  val dY = (destY - point.geom.y)
                  val d = dX * dX + dY * dY
                  if (d < rr) {
                    val w = 1 / d
                    s += point.data * w
                    ws += w
                    c += 1
                  }
                }

                if (c == 0) {
                  tile.set(col, row, NODATA)
                } else {
                  val mean = s / ws
                  tile.set(col, row, mean.toInt)
                }
              }
            }
          }
        case None =>
          val length = points.size
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val destX = re.gridColToMap(col)
              val destY = re.gridRowToMap(row)
              var s = 0.0
              var c = 0
              var ws = 0.0

              cfor(0)(_ < length, _ + 1) { i =>
                val point = points(i)
                val dX = (destX - point.geom.x)
                val dY = (destY - point.geom.y)
                val d = dX * dX + dY * dY
                val w = 1 / d
                s += point.data * w
                ws += w
                c += 1
              }

              if (c == 0) {
                tile.set(col, row, NODATA)
              } else {
                val mean = s / ws
                tile.set(col, row, mean.toInt)
              }
            }
          }
      }
      tile
    }
  }

  def rasterize(feature: Geometry, rasterExtent: RasterExtent)(f: Transformer[Int]): Tile =
    Rasterizer.rasterize(feature, rasterExtent)(f)

  def rasterize(feature: Geometry, rasterExtent: RasterExtent, value:Int): Tile =
    Rasterizer.rasterizeWithValue(feature, rasterExtent, value)

/**
 * Gives a raster that represents the number of occuring points per cell.
 * 
 *  @param points               Sequence of points to be counted.
 *  @param rasterExtent         RasterExtent of the resulting raster.
 * 
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
