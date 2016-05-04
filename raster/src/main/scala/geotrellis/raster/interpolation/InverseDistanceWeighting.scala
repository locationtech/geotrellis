package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector._
import spire.syntax.cfor._

object InverseDistanceWeighting {
  /**
    * Compute an Inverse Distance Weighting raster over the given
    * extent from the given set known-points.  Please see
    * https://en.wikipedia.org/wiki/Inverse_distance_weighting for
    * more details.
    *
    * @param   points            A collection of known-points
    * @param   rasterExtent      The study area
    * @param   radius            Interpolation radius in coordinate unit
    * @param   equalWeightRadius If any points lie at that distance from a cell, the interpolation result for it would
    *                            be the mean of those points
    * @param   cellType          Interpolation radius in coordinate unit
    * @param   onSet             Function to be applied before storing interpolation values
    * @return The data interpolated across the study area
    */
  def apply[D](
                points: Seq[PointFeature[D]],
                rasterExtent: RasterExtent,
                radius: Double = Double.PositiveInfinity,
                equalWeightRadius: Double = 0,
                cellType: CellType = IntConstantNoDataCellType,
                onSet: Double => Double = x => x
              )(implicit ev: D => Double): Tile = {
    val cols = rasterExtent.cols
    val rows = rasterExtent.rows
    val tile = ArrayTile.empty(cellType, cols, rows)

    if (points.isEmpty) {
      tile
    } else {
      val ewr2 = equalWeightRadius * equalWeightRadius

      def idw(points: Seq[PointFeature[D]], x: Double, y: Double)(filter: Double => Boolean) = {
        var sum = 0.0
        var count = 0
        var weightSum = 0.0
        var sampleSum = 0.0
        var sampleCount = 0

        for (point <- points) {
          val dX = x - point.geom.x
          val dY = y - point.geom.y
          val d2 = dX * dX + dY * dY

          if (filter(d2)) {
            if (d2 <= ewr2) {
              sampleSum += point.data
              sampleCount += 1
            } else {
              val w = 1 / d2
              sum += point.data * w
              weightSum += w
              count += 1
            }
          }
        }

        if (sampleCount == 0) {
          if (count == 0) {
            doubleNODATA
          } else {
            onSet(sum / weightSum)
          }
        } else {
          onSet(sampleSum / sampleCount)
        }
      }

      radius match {
        case Double.PositiveInfinity =>
          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val x = rasterExtent.gridColToMap(col)
              val y = rasterExtent.gridRowToMap(row)
              val value = idw(points, x, y)(d2 => true)

              tile.setDouble(col, row, value)
            }
          }
        case _ =>
          val r2 = radius * radius
          val index: SpatialIndex[PointFeature[D]] = SpatialIndex(points)(p => (p.geom.x, p.geom.y))

          cfor(0)(_ < rows, _ + 1) { row =>
            cfor(0)(_ < cols, _ + 1) { col =>
              val x = rasterExtent.gridColToMap(col)
              val y = rasterExtent.gridRowToMap(row)
              val rPoints = index.pointsInExtent(Extent(x - radius, y - radius, x + radius, y + radius))
              val value = if (rPoints.isEmpty) {
                doubleNODATA
              } else {
                idw(rPoints, x, y)(d2 => d2 <= r2)
              }

              tile.setDouble(col, row, value)
            }
          }
      }
      tile
    }
  }
}