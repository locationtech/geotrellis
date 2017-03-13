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

package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import spire.syntax.cfor._

object InverseDistanceWeighted {
  case class Options(
    radiusX: Double = Double.PositiveInfinity,
    radiusY: Double = Double.PositiveInfinity,
    rotation: Double = 0.0,
    weightingPower: Double = 2.0,
    smoothingFactor: Double = 0.0,
    equalWeightRadius: Double = 0.0000000000001,
    cellType: CellType = IntConstantNoDataCellType,
    onSet: Double => Double = x => x
  )

  object Options {
    def DEFAULT = Options()
  }

  /**
    * Compute an Inverse Distance Weighting raster over the given
    * extent from the given set known-points.  Please see
    * https://en.wikipedia.org/wiki/Inverse_distance_weighting for
    * more details. Implementation taken partially from GDAL GDALGridInverseDistanceToAPower
    *
    * @param   points            A collection of known-points
    * @param   rasterExtent      The study area
    * @param   radius            Interpolation radius in coordinate unit
    * @param   equalWeightRadius If any points lie at that distance from a cell, the interpolation result for it would
    *                            be the mean of those points
    * @param   cellType          Interpolation radius in coordinate unit
    * @param   onSet             Function to be applied before storing interpolation values
    *
    * @return The data interpolated across the study area
    */
  def apply[D](
    points: Traversable[PointFeature[D]],
    rasterExtent: RasterExtent,
    options: Options = Options.DEFAULT
  )(implicit ev: D => Double): Raster[Tile] = {
    val Options(radiusX, radiusY, rotation, weightingPower, smoothingFactor, equalWeightRadius, cellType, onSet) = options

    val cols = rasterExtent.cols
    val rows = rasterExtent.rows
    val tile = ArrayTile.empty(cellType, cols, rows)

    if (!points.isEmpty) {
      val ewr2 = equalWeightRadius * equalWeightRadius
      val radiusXY = radiusX * radiusY

      def idw(points: Traversable[PointFeature[D]], x: Double, y: Double, hasRadius: Boolean) = {
        var sum = 0.0
        var count = 0
        var weightSum = 0.0
        var sampleSum = 0.0
        var sampleCount = 0
        val halfPow = weightingPower / 2.0

        for (point <- points) {
          val dX = x - point.geom.x
          val dY = y - point.geom.y
          val d2 = dX * dX + dY * dY + smoothingFactor * smoothingFactor

          val (dX2, dY2) =
            if(rotation != 0.0) {
              val rotRads = math.toRadians(rotation)
              val coeff1 = math.cos(rotRads)
              val coeff2 = math.sin(rotRads)
              val dXRotated = dX * coeff1 + dY * coeff2;
              val dYRotated = dY * coeff1 - dX * coeff2;
              (dXRotated, dYRotated)
            } else {
              (dX, dY)
            }

          if (!hasRadius || radiusY * dX2 * dX2 + radiusX * dY2 * dY2 <= radiusXY ) {
            val data = point.data: Double
            if (d2 <= ewr2) {
              sampleSum += data
              sampleCount += 1
            } else {
              val powerWeight = math.pow(d2, halfPow)
              val w = 1 / powerWeight
              sum += data * w
              weightSum += w
              count += 1
            }
          }
        }

        if (sampleCount == 0) {
          if (count == 0) {
            Double.NaN
          } else {
            onSet(sum / weightSum)
          }
        } else {
          onSet(sampleSum / sampleCount)
        }
      }

      (radiusX * radiusY) match {
        case x if x.isInfinite =>
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val x = rasterExtent.gridColToMap(col)
              val y = rasterExtent.gridRowToMap(row)
              val value = idw(points, x, y, false)

              tile.setDouble(col, row, value)
            }
          }
        case r2 =>
          val index: SpatialIndex[PointFeature[D]] = SpatialIndex(points)(p => (p.geom.x, p.geom.y))

          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val x = rasterExtent.gridColToMap(col)
              val y = rasterExtent.gridRowToMap(row)
              val rPoints = index.pointsInExtent(Extent(x - radiusX, y - radiusY, x + radiusX, y + radiusY))
              val value = if (rPoints.isEmpty) {
                doubleNODATA
              } else {
                idw(rPoints, x, y, true)
              }

              tile.setDouble(col, row, value)
            }
          }
      }
    }

    Raster(tile, rasterExtent.extent)
  }
}
