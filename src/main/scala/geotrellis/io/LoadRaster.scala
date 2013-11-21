package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
 * Load the raster data for a particular extent/resolution for the 
 * raster layer in the catalog with name 'n'
 */
case class LoadRaster(n:Op[String], r:Op[RasterExtent]) extends Op[Raster] {
  def _run(context:Context) = runAsync(List(n, r, context))
  val nextSteps:Steps = {
    case (name:String) :: null :: (context:Context) :: Nil => 
     context.getRasterByName(name, None)
    case (name:String) :: (re:RasterExtent) :: (context:Context) :: Nil => 
      context.getRasterByName(name, Some(re))
  }
}

object LoadRaster {
  def apply(n:Op[String]):LoadRaster = {
    LoadRaster(n, Literal[RasterExtent](null))
  }
}
