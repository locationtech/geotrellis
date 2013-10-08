package geotrellis.io

import geotrellis._
import geotrellis.process._
import geotrellis.source._

/**
  * Load the [[RasterDefinition]] from the raster layer with the specified name.
  */
case class LoadRasterDefinition(n:Op[String]) extends Op[RasterDefinition] {
  def _run(context:Context) = runAsync(List(n, context))
  val nextSteps:Steps = {
    case (n:String) :: (context:Context) :: Nil => {
      val info = context.getRasterLayerInfo(n)
      Result(RasterDefinition(n,info.rasterExtent,info.tileLayout))
    }
  }
}
