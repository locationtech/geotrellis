package geotrellis.raster.op.focal

import geotrellis._

import Angles._

case class Slope(r:Op[Raster], zFactorOp:Op[Double]) extends FocalOp1[Double,Raster](r,Square(1),zFactorOp)({
  (r,n) => new SurfacePointCalculation[Raster] with DoubleRasterDataResult 
                                               with Initialization1[Double] {
    var zFactor = 0.0

    override def init(r:Raster,z:Double) = {
      super.init(r)
      zFactor = z
    }

    def setValue(x:Int,y:Int,s:SurfacePoint) {
      data.setDouble(x,y,degrees(s.slope(zFactor)))
    }
  }
})
