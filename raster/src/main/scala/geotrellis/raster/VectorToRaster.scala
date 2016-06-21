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
    * Compute an Inverse Distance Weighting raster over the given
    * extent from the given set known-points.  Please see
    * https://en.wikipedia.org/wiki/Inverse_distance_weighting for
    * more details.
    */
  def idwInterpolate(points: Seq[PointFeature[Int]], re: RasterExtent): Tile =
    idwInterpolate(points, re, None)

  /**
    * Compute an Inverse Distance Weighting raster over the given
    * extent from the given set known-points.  Please see
    * https://en.wikipedia.org/wiki/Inverse_distance_weighting for
    * more details.
    */
  def idwInterpolate(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Int): Tile =
    idwInterpolate(points, re, Some(radius))

  /**
    * Compute an Inverse Distance Weighting raster over the given
    * extent from the given set known-points.  Please see
    * https://en.wikipedia.org/wiki/Inverse_distance_weighting for
    * more details.
    *
    * @param   points  A collection of known-points
    * @param   re      The study area
    * @return          The data interpolated across the study area
    */
  def idwInterpolate(points: Seq[PointFeature[Int]], re: RasterExtent, radius: Option[Int]): Tile = {
    val cols = re.cols
    val rows = re.rows
    val tile = ArrayTile.empty(IntConstantNoDataCellType, cols, rows)
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
