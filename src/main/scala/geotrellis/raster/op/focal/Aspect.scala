package geotrellis.raster.op.focal

import geotrellis._

case class Aspect(r:Op[Raster]) extends FocalOp[Raster](r,Square(1))({
  (r,n) => new CellwiseCalculation[Raster] with DoubleRasterDataResult 
                                           with SurfacePointCalculation {
    var cellWidth = 0.0
    var cellHeight = 0.0
    var cols = 0
    val s = new SurfacePoint
    var y = -1

    override val traversalStrategy = Some(TraversalStrategy.ScanLine)

    override def init(r:Raster) = {
      super.init(r)
      cols = r.cols
      cellWidth = r.rasterExtent.cellwidth
      cellHeight = r.rasterExtent.cellheight
    }

    def reset() = { y += 1 ; resetCols(y) }
    def remove(r:Raster,x:Int,y:Int) = { }

    def setValue(x:Int,y:Int) { 
      calcSurface(s,cellWidth,cellHeight)
      data.setDouble(x,y,s.aspect) 
      moveRight(x+1 == cols)
    }
  }
})
