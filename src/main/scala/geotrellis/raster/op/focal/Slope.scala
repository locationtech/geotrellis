package geotrellis.raster.op.focal

import geotrellis._

case class Slope(r:Op[Raster], zFactorOp:Op[Double]) extends FocalOp1[Double,Raster](r,Square(1),zFactorOp)({
  (r,n) => new CellwiseCalculation with DoubleRasterDataResult 
                                   with Initialization1[Double] 
                                   with SurfacePointCalculation {
    var zFactor = 0.0
    var cellWidth = 0.0
    var cellHeight = 0.0
    var cols = 0
    val s = new SurfacePoint
    var y = -1

    override val traversalStrategy = Some(TraversalStrategy.ScanLine)

    override def init(r:Raster,z:Double) = {
      super.init(r)
      zFactor = z
      cols = r.cols
      cellWidth = r.rasterExtent.cellwidth
      cellHeight = r.rasterExtent.cellheight
    }

    def reset() = { y += 1 ; resetCols(y) }
    def remove(r:Raster,x:Int,y:Int) = { }

    def setValue(x:Int,y:Int) {
      calcSurface(s,cellWidth,cellHeight)
      data.setDouble(x,y,s.slope(zFactor)) 
      moveRight(x+1 == cols)
    }
  }
})
