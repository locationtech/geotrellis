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

package geotrellis.pointcloud.spark.dem

import io.pdal._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.vector._
import spire.syntax.cfor._

object PointToGrid {
  trait Filter {
    def apply(x: Double, y: Double, z: Double): Boolean
  }

  case class Options(
    radius: Double = 8.4852813742385713,
    smoothingFactor: Double = 0.0,
    weightingPower: Double = 2.0,
    performFill: Boolean = true,
    fillSize: Int = 1,
    filter: Option[Filter] = None,
    checkBounds: Boolean = true,
    cellType: CellType = DoubleConstantNoDataCellType
  )

  object Options {
    def DEFAULT = Options()
  }

  def createRaster(pointCloud: PointCloud, re: RasterExtent): Raster[Tile] =
    createRaster(pointCloud, re, Options.DEFAULT)

  def createRaster(pointCloud: PointCloud, re: RasterExtent, options: Options): Raster[Tile] = {
    val interp = new InCoreInterp(re, options.cellType)
    interpolatePoints(interp, pointCloud, options)
    val result =
      if(options.performFill) {
        interp.result.focalMean(Square(options.fillSize), target = TargetCell.NoData)
      } else {
        interp.result
      }

    Raster(result, re.extent)
  }

  def createRaster(pointClouds: Iterable[PointCloud], re: RasterExtent): Raster[Tile] =
    createRaster(pointClouds, re, Options.DEFAULT)

  def createRaster(pointClouds: Iterable[PointCloud], re: RasterExtent, options: Options): Raster[Tile] = {
    val interp = new InCoreInterp(re)

    for(pointCloud <- pointClouds) {
      interpolatePoints(interp, pointCloud, options)
    }

    val result =
      if(options.performFill) {
        interp.result.focalMean(Square(options.fillSize), target = TargetCell.NoData)
      } else {
        interp.result
      }

    Raster(result, re.extent)
  }

  private def interpolatePoints(
    interp: InCoreInterp,
    pointCloud: PointCloud,
    options: Options = Options.DEFAULT
  ): Unit = {
    options.filter match {
      case Some(filter) =>
        if(options.checkBounds) {
          cfor(0)(_ < pointCloud.length, _ + 1) { i =>
            val x = pointCloud.getX(i)
            val y = pointCloud.getY(i)

            if(interp.inBounds(x, y)) {
              val z = pointCloud.getZ(i)

              if(filter(x, y, z)) {
                interp.update(x, y, z)
              }
            }
          }
        } else {
          cfor(0)(_ < pointCloud.length, _ + 1) { i =>
            val x = pointCloud.getX(i)
            val y = pointCloud.getY(i)
            val z = pointCloud.getZ(i)

            if(filter(x, y, z)) {
              interp.update(x, y, z)
            }
          }
        }
      case None =>
        if(options.checkBounds) {
          cfor(0)(_ < pointCloud.length, _ + 1) { i =>
            val x = pointCloud.getX(i)
            val y = pointCloud.getY(i)

            if(interp.inBounds(x, y)) {
              val z = pointCloud.getZ(i)

              interp.update(x, y, z)
            }
          }
        } else {
          cfor(0)(_ < pointCloud.length, _ + 1) { i =>
            val x = pointCloud.getX(i)
            val y = pointCloud.getY(i)
            val z = pointCloud.getZ(i)

            interp.update(x, y, z)
          }
        }
    }
  }
}

/**
  * Code based on points2dem project
  * as well as GDAL's IDW interpolation.
  */
class InCoreInterp(
  rasterExtent: RasterExtent,
  cellType: CellType = DoubleConstantNoDataCellType,
  radius: Double = 8.4852813742385713,
  smoothingFactor: Double = 0.0,
  weightingPower: Double = 2.0
)  {
  val halfPow = weightingPower / 2.0

  val CellSize(cw, ch) = rasterExtent.cellSize
  val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)
  val Extent(xmin, ymin, xmax, ymax) = rasterExtent.extent

  // The number of columns potentially influenced
  // back and forward from each point
  val windowCols =
    math.ceil(radius / cw).toInt

  val windowRows =
    math.ceil(radius / ch).toInt

  val valueSumTile = ArrayTile.empty(cellType, cols, rows)
  val weightSumTile = ArrayTile.empty(cellType, cols, rows)

  val r2 = radius * radius

  val bounds = Extent(xmin - radius, ymin - radius, xmax + radius, ymax + radius)
  def inBounds(x: Double, y: Double): Boolean =
    bounds.contains(x, y)

  def update(x: Double, y: Double, z: Double): Unit = {
    val col = rasterExtent.mapXToGrid(x)
    val row = rasterExtent.mapYToGrid(y)

    val targetColMin: Int =
      math.max(col - windowCols, 0)

    val targetColMax: Int =
      math.min(col + windowCols, cols - 1)

    val targetRowMin: Int =
      math.max(row - windowRows, 0)

    val targetRowMax: Int =
      math.min(row + windowRows, rows - 1)

    cfor(targetColMin)(_ <= targetColMax, _ + 1) { targetCol =>
      cfor(targetRowMin)(_ <= targetRowMax, _ + 1) { targetRow =>
        val targetX = rasterExtent.gridColToMap(targetCol)
        val targetY = rasterExtent.gridRowToMap(targetRow)

        val dX = x - targetX
        val dY = y - targetY
        val d2 = dX * dX + dY * dY + smoothingFactor * smoothingFactor

        if (radius * dX * dX + radius * dY * dY <= r2 ) {
          val powerWeight = math.pow(d2, halfPow)
          val w = 1 / powerWeight
          val valueSum = {
            val thisValue = z * w
            val existingValue = valueSumTile.getDouble(targetCol, targetRow)
            if(isData(existingValue)) {
              existingValue + thisValue
            } else {
              thisValue
            }
          }

          val weightSum = {
            val existingValue = weightSumTile.getDouble(targetCol, targetRow)
            if(isData(existingValue)) {
              existingValue + w
            } else {
              w
            }
          }
          valueSumTile.setDouble(targetCol, targetRow, valueSum)
          weightSumTile.setDouble(targetCol, targetRow, weightSum)
        }
      }
    }
  }

  def result: Tile =
    valueSumTile / weightSumTile
}
