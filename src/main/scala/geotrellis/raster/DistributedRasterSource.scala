package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._
import geotrellis.SourceBuilder
 
object DistributedRasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[DistributedRasterSource,Raster,DistributedRasterSource] = new CanBuildSourceFrom[DistributedRasterSource,Raster,DistributedRasterSource] {
    def apply() = new DistributedRasterSourceBuilder
    def apply(from:DistributedRasterSource, op:Op[Seq[Op[Raster]]]) = new DistributedRasterSourceBuilder
  }
}
  class DistributedRasterSourceBuilder extends SourceBuilder[Raster, DistributedRasterSource] {
    def result = new DistributedRasterSource(null)
  }

  class DistributedRasterSource(seqOp:Op[Seq[Op[Raster]]]) extends RasterSource(seqOp) with RasterSourceLike[DistributedRasterSource] {}

  
object Foo {
  def main() = {    
    val d1 = new DistributedRasterSource(null)
    val d2:DistributedRasterSource = d1.map(local.Add(_,3))  
    val d3:DistributedRasterSource = d1.localAdd(3)

    val l1 = new LocalRasterSource(null)
    val l2:LocalRasterSource = l1.map(local.Add(_,3))

    val l3:LocalRasterSource = l1.localAdd(2)
  }
}
