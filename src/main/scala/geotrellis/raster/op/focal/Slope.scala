package geotrellis.raster.op.focal

import geotrellis._

import Angles._

/** Creates [[Slope]] instances. */
object Slope {
  /**
   * Creates a slope operation with a default zFactor of 1.0.
   *
   * @param   raster     Raster for which to compute the aspect.
   */
  def apply(r:Op[Raster]):Slope = {
    Slope(r,1.0)
  }

  /**
   * Creates a slope operation.
   *
   * @param   raster     Raster for which to compute the aspect.
   * @param   zFactor    Number of map units to one elevation unit.
   *                     The z factor is the multiplicative factor to convert elevation units
   *  
   */
  def apply(r:Op[Raster], zFactor:Op[Double]):Slope = {
    new Slope(r,zFactor)
  }
}

/** Calculates the slope of each cell in a raster.
  *
  * Slope is the magnitude portion of the gradient vector. It is the maximum
  * change of elevation from a raster cell to any immediate neighbor. It uses Horn's method
  * for computing slope.
  * 
  * As with aspect, slope is calculated from estimates of the partial derivatives dz/dx and dz/dy.
  *
  * Slope is computed in degrees from horizontal.
  * 
  * The expression for slope is:
  * {{{
  * val slope = atan(sqrt(pow(`dz/dy`,2) * pow(`dz/dx`,2)))
  * }}}
  *
  * @param   raster     Raster for which to compute the aspect.
  * @param   zFactor    Number of map units to one elevation unit.
  *                     The z factor is the multiplicative factor to convert elevation units
  * 
  * @see [[SurfacePoint]] for slope calculation logic.
  * @see [[http://goo.gl/JCnNP Geospatial Analysis - A comprehensive guide]]
  * (Smit, Longley, and Goodchild)
  */
class Slope(r:Op[Raster], zFactor:Op[Double]) extends FocalOp1[Double,Raster](r,Square(1),zFactor)({
  (r,n) => new SurfacePointCalculation[Raster] with DoubleRasterDataResult 
                                               with Initialization1[Double] {
    var _zFactor = 0.0

    override def init(r:Raster,z:Double) = {
      super.init(r)
      _zFactor = z
    }

    def setValue(x:Int,y:Int,s:SurfacePoint) {
      data.setDouble(x,y,degrees(s.slope(_zFactor)))
    }
  }
})
