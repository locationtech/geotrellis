package geotrellis.io

import geotrellis.process._
import geotrellis._

/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */
case class LoadFile(p:Op[String]) extends Operation[Raster] {
  def _run(context:Context) = runAsync(List(p,context))
  val nextSteps:Steps = {
    case (path:String) :: (context:Context) :: Nil =>
      context.getRasterStepOutput(path, None, None)
  }
}

/**
 * Load the raster data from the specified file, using the RasterExtent provided.
 */
case class LoadFileWithRasterExtent(p:Op[String], e:Op[RasterExtent]) extends Operation[Raster] {
  def _run(context:Context) = runAsync(List(p,e, context))
  val nextSteps:Steps = {
    case (path:String) :: (re:RasterExtent) :: (context:Context) :: Nil => 
      context.getRasterStepOutput(path, None, Some(re))
  }
}

object LoadFile {
  def apply(p:Op[String], e:Op[RasterExtent]) = LoadFileWithRasterExtent(p, e)
}
