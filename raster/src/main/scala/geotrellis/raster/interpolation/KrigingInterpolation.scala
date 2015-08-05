/*
 * Copyright (c) 2015 Azavea.
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

package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.interpolation._

import spire.syntax.cfor._

object KrigingInterpolation {

  private def isValid(point: Point, re: RasterExtent): Boolean =
    point.x >= re.extent.xmin && point.x <= re.extent.xmax && point.y <= re.extent.ymax && point.y >= re.extent.ymin

  def apply(method: KrigingVectorBase, points: Seq[PointFeature[Double]], re: RasterExtent, maxdist: Double, binmax: Double, model: ModelType): Tile = {
    model match {
      case Linear(_,_) => throw new UnsupportedOperationException("Linear semivariogram does not accept maxDist and maxBin values")
      case _ =>
        val cols = re.cols
        val rows = re.rows
        val tile = ArrayTile.alloc(TypeDouble, cols, rows)
        if(points.isEmpty)
          throw new UnsupportedOperationException("The set of points for constructing the prediction is empty")
        else {
          val rasterData: Array[Point] = Array.tabulate(rows * cols){i => Point(i/cols, i%cols)}
          val prediction: Array[(Double, Double)] = method.predict(rasterData)
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              tile.setDouble(col, row, prediction(row*cols + col)._1)
            }
          }
          tile
        }
    }
  }

  def apply(method: KrigingVectorBase, points: Array[PointFeature[Double]], re: RasterExtent, chunkSize: Double, model: ModelType): Tile = {
    model match {
      case Linear(radius, lag) =>
        val cols = re.cols
        val rows = re.rows
        val tile = ArrayTile.alloc(TypeDouble, cols, rows)
        if (points.isEmpty) {
          throw new IllegalArgumentException("The set of points for constructing the prediction is empty")
        } else {
          val funcInterp = method.createPredictor()
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val (x, y) = re.gridToMap(col, row)
              val (v, _) = funcInterp(Point(x, y))

              tile.setDouble(col, row, v)
            }
          }
          tile
        }
      case _ => throw new UnsupportedOperationException("Non linear semivariograms do not accept radii and lags")
    }
  }
}
