package trellis.operation

import trellis.{Extent,RasterExtent}
import trellis.process._

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class CropRasterExtentByExtent(g:Op[RasterExtent], e:Op[Extent])
extends Op[RasterExtent] {
  def childOperations = List(g, e)

  def _run(server:Server) = runAsync(List(g, e), server)

  val nextSteps:Steps = {
    case (geo:RasterExtent) :: (ext:Extent) :: Nil => step2(geo, ext)
  }

  def step2(geo:RasterExtent, ext:Extent) = {
    val cols = ((ext.ymax - ext.ymin) / geo.cellheight).toInt
    val rows = ((ext.xmax - ext.xmin) / geo.cellwidth).toInt
    Some(RasterExtent(ext, geo.cellwidth, geo.cellheight, cols, rows))
  }
}
