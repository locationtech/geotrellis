package geotrellis.raster.op.focal

import geotrellis._

import Angles._

case class Aspect(r:Op[Raster]) extends FocalOp[Raster](r,Square(1))({
  (r,n) => new SurfacePointCalculation[Raster] with DoubleRasterDataResult {
    def setValue(x:Int,y:Int,s:SurfacePoint) {
      data.setDouble(x,y,degrees(s.aspect))
    }
  }
})
