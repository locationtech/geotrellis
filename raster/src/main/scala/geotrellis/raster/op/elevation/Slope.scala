package geotrellis.raster.op.elevation

import geotrellis.raster._
import geotrellis.raster.op.focal._
import geotrellis.raster.op.focal.Angles._

/**
 * Calculates the slope of each cell in a raster.
 *
 * Slope is the magnitude portion of the gradient vector. It is the maximum
 * change of elevation from a raster cell to any immediate neighbor. It uses Horn's method
 * for computing slope.
 *
 * As with aspect, slope is calculated from estimates of the partial derivatives dz / dx and dz / dy.
 *
 * Slope is computed in degrees from horizontal.
 *
 * If Slope operations encounters NoData in its neighborhood, that neighborhood cell well be treated as having
 * the same elevation as the focal cell.
 *
 * The expression for slope is:
 * {{{
 * val slope = atan(sqrt(pow(`dz / dy`, 2) * pow(`dz / dx`, 2)))
 * }}}
 *
 */
object Slope {

  def apply(r: Tile, n: Neighborhood, bounds: Option[GridBounds], cs: CellSize, z: Double): Tile = {
    new SurfacePointCalculation[Tile](r, n, bounds, cs)
      with DoubleArrayTileResult
    {
      val zFactor = z

      def setValue(x: Int, y: Int, s: SurfacePoint) {
        tile.setDouble(x, y, degrees(s.slope(zFactor)))
      }
    }
  }.execute()
}
