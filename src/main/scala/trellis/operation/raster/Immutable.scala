package trellis.operation

import trellis.process._
import trellis.raster.IntRaster

/**
 * Wraps an operation returning an IntRaster and caches the result.
 * The name is misleading--it does not make operations immutable.
 *
 * Deprecated!
 */
case class Immutable(r:IntRasterOperation)
extends SimpleOperation[IntRaster] with CachedOperation[IntRaster] {
  def _value(context:Context) = context.run(r)
}
