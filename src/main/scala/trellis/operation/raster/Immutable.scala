package trellis.operation

import trellis.process._
import trellis.raster.IntRaster

/**
 * Wraps an operation returning an IntRaster and caches the result.
 * The name is misleading--it does not make operations immutable.
 *
 * Deprecated!
 */
case class Immutable(r:IntRasterOperation) extends IntRasterOperation with SimpleOperation[IntRaster] with CachedOperation[IntRaster] {
  override def childOperations = List(r)
  def _value(server:Server)(implicit t:Timer) = server.run(r)
}
