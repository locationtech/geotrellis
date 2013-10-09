package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait LocalOpMethods[+Repr <: RasterSource] { self: Repr =>
  /** Add a constant Int value to each cell. See [[AddConstant]] */
  def localAdd(i: Int) = self.mapOp(Add(_, i))
  /** Add a constant Double value to each cell. See [[AddDoubleConstant]]  */
  def localAdd(d: Double) = self.mapOp(Add(_, d))
  /** Add the values of each cell in each raster. See [[AddRasters]] */
//  def localAdd(rs:RasterSource) = self.combineOp(rs)(Add(_,_))

  def localSubtract(i: Int):RasterSource = self.mapOp(Subtract(_, i))
}
