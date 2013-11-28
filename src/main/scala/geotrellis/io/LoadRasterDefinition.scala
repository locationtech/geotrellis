package geotrellis.io

import geotrellis._
import geotrellis.process._
import geotrellis.source._

object LoadRasterDefinition {
  def apply(n:Op[String]):LoadRasterDefinition =
    LoadRasterDefinition(None,n)

  def apply(ds:String,n:Op[String]):LoadRasterDefinition =
    LoadRasterDefinition(Some(ds),n)
}

/**
  * Load the [[RasterDefinition]] from the raster layer with the specified name.
  */
case class LoadRasterDefinition(ds: Op[Option[String]], n: Op[String]) extends Op[RasterDefinition] {
  def _run(context:Context) = runAsync(List(ds, n, context))
  val nextSteps:Steps = {
    case (ds:Option[String]) :: (n:String) :: (context:Context) :: Nil => {
      val info = context.getRasterLayer(ds,n).info
      Result(RasterDefinition(n,info.rasterExtent,info.tileLayout))
    }
  }
}
