package geotrellis.raster.op.hydrology

import geotrellis._
import geotrellis.raster._
import geotrellis.source._
import geotrellis.raster.op.focal.Square

trait HydrologyOpMethods[+Repr <: RasterSource] { self: Repr =>
  def accumulation() = globalOp(Accumulation(_))
  def fill(options: FillOptions) = focal(Square(1)) { (r,n,t) => Fill(r, options, t) }
  def flowDirection() = globalOp(FlowDirection(_))
}
