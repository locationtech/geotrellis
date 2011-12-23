package trellis.operation

import trellis.process.{Server,Results}
import trellis.raster.IntRaster

/**
 * Given an operation producing a raster, returns a copy of this raster.
 *
 * Useful because some operations currently mutate one or more of their
 * arguments.
 */
case class CopyRaster(r:IntRasterOperation) extends IntRasterOperation {
  def childOperations = List(r)
  def _run(server:Server, callback:Callback) = {
    runAsync(List(r),server,callback)
  }

  val nextSteps:Steps = {
    case Results(List(r:IntRaster)) => Some(r.copy)
  }

}
