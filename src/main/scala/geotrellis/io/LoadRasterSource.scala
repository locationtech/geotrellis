package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */
  //TODO: handle rasterextent
/*
case class LoadRasterSource(n:Op[String]) extends Op[Raster] {
  def _run(context:Context) = runAsync(List(n, r, context))
  val nextSteps:Steps = {
    case (name:String) :: null :: (context:Context) :: Nil => 
     context.getRasterByName(name, None)
    case (name:String) :: (re:RasterExtent) :: (context:Context) :: Nil => 
      context.getRasterByName(name, Some(re))
  }
}
*/
