package geotrellis.raster.op.global

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait GlobalOpMethods[+Repr <: RasterSource] { self: Repr =>
  def rescale(newMin:Int,newMax:Int) = {
    val minMax = self.minMax.get
    self.globalOp { r =>
      minMax.map { case (min,max) => 
        r.normalize(min,max,newMin,newMax)
      }
    }
  }

  def toVector() = 
    self.converge.mapOp(ToVector(_))

  def asArray() = 
    self.converge.mapOp(AsArray(_))

  def regionGroup(options:RegionGroupOptions = RegionGroupOptions.Default) =
    self.converge.mapOp(RegionGroup(_,options))

  def verticalFlip() =
    self.globalOp(VerticalFlip(_))

  def costDistance(points: Seq[(Int,Int)]) = 
    self.globalOp(CostDistance(_,points))

  def convolve(kernel:Kernel) =
    self.globalOp(Convolve(_,kernel))
}
