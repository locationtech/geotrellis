package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait LocalOpMethods[+Repr <: RasterSource] 
  extends AddOpMethods[Repr]
     with SubtractOpMethods[Repr]
     with MultiplyOpMethods[Repr]
     with DivideOpMethods[Repr] { self: Repr => 
  def localCombinations(rs:RasterSource) = combineOp(rs)(Combination(_,_))
}
