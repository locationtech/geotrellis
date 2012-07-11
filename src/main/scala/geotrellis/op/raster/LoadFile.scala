package geotrellis.op.raster

import geotrellis.RasterExtent
import geotrellis.process._
import geotrellis.Raster
import geotrellis._
import geotrellis.op._


/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */
case class LoadFile(p:Op[String]) extends Operation[Raster] {
  def _run(context:Context) = runAsync(List(p,context))
  val nextSteps:Steps = {
    case (path:String) :: (context:Context) :: Nil =>
      Result(context.loadRaster(path, null))
  }
}

case class LoadFileWithRasterExtent(p:Op[String], e:Op[RasterExtent]) extends Operation[Raster] {
  def _run(context:Context) = runAsync(List(p,e, context))
  val nextSteps:Steps = {
    case (path:String) :: (re:RasterExtent) :: (context:Context) :: Nil => 
      Result(context.loadRaster(path, re))
  }
}

object LoadFile {
  def apply(p:Op[String], e:Op[RasterExtent]) = LoadFileWithRasterExtent(p, e)
}
