package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import Angles._

/** Calculates the aspect of each cell in a raster.
  *
  * Aspect is the direction component of a gradient vector. It is the
  * direction in degrees of which direction the maximum change in direction is pointing.
  * It is defined as the directional component of the gradient vector and is the
  * direction of maximum gradient of the surface at a given point. It uses Horn's method
  * for computing aspect.
  *
  * As with slope, aspect is calculated from estimates of the partial derivatives dz/dx and dz/dy.
  *
  * Aspect is computed in degrees from due north, i.e. as an azimuth in degrees not radians.
  * The expression for aspect is:
  * {{{
  * val aspect = 270 - 360/(2*Pi) * atan2(`dz/dy`, - `dz/dx`)
  * }}}
  * @param   raster     Raster for which to compute the aspect.
  *
  * @see [[SurfacePoint]] for aspect calculation logic.
  * @note Paraphrased from
  * [[http://goo.gl/JCnNP Geospatial Analysis - A comprehensive guide]]
  * (Smit, Longley, and Goodchild)
  */
case class Aspect(r:Op[Raster],neighbors:Op[TileNeighbors]) 
    extends FocalOp[Raster](r,Square(1),neighbors)({
  (r,n) => new SurfacePointCalculation[Raster] with DoubleRasterDataResult {
    def setValue(x:Int,y:Int,s:SurfacePoint) {
      data.setDouble(x,y,degrees(s.aspect))
    }
  }
}) with FocalOperation[Raster]

object Aspect {
  def apply(r:Op[Raster]):Aspect = Aspect(r,Literal(TileNeighbors.NONE))
}
