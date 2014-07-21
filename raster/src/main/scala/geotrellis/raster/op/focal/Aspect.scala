package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.raster.op.focal.Angles._

/** Calculates the aspect of each cell in a raster.
  *
  * Aspect is the direction component of a gradient vector. It is the
  * direction in degrees of which direction the maximum change in direction is pointing.
  * It is defined as the directional component of the gradient vector and is the
  * direction of maximum gradient of the surface at a given point. It uses Horn's method
  * for computing aspect.
  *
  * As with slope, aspect is calculated from estimates of the partial derivatives dz / dx and dz / dy.
  *
  * Aspect is computed in degrees from due north, i.e. as an azimuth in degrees not radians.
  * The expression for aspect is:
  * {{{
  * val aspect = 270 - 360 / (2 * Pi) * atan2(`dz / dy`, - `dz / dx`)
  * }}}
  *
  */
object Aspect {

  def apply(tile: Tile, n: Neighborhood, cs: CellSize): FocalCalculation[Tile] = {
    new SurfacePointCalculation[Tile](tile, n, cs)
      with DoubleArrayTileResult
    {
      init(r)
      def setValue(x: Int, y: Int, s: SurfacePoint) {
        tile.setDouble(x, y, degrees(s.aspect))
      }
    }
  }
}