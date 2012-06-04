package geotrellis.operation

import geotrellis.RasterExtent
import geotrellis.process._
import geotrellis.Raster

/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */
case class LoadRaster(n:Op[String], r:Op[RasterExtent]) extends Op[Raster] {
  def _run(context:Context) = runAsync(List(n, r, context))
  val nextSteps:Steps = {
    case (name:String) :: null :: (context:Context) :: Nil => {
      Result(context.getRasterByName(name, null))
    }
    case (name:String) :: (re:RasterExtent) :: (context:Context) :: Nil => {
      Result(context.getRasterByName(name, re))
    }
  }
}

object LoadRaster {
  def apply(n:Op[String]):LoadRaster = {
    LoadRaster(n, Literal[RasterExtent](null))
  }
}
