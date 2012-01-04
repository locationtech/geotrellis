package trellis.operation

import scala.math.{ceil, floor}
import trellis.{Extent,RasterExtent}
import trellis.process._

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class CropRasterExtent(g:Op[RasterExtent],
                            xmin:Double, ymin:Double,
                            xmax:Double, ymax:Double) extends Op[RasterExtent] {
  def childOperations = List(g)

  def _run(context:Context) = runAsync(List(g))

  val nextSteps:Steps = {
    case (geo:RasterExtent) :: Nil => step2(geo)
  }

  def step2(geo:RasterExtent) = {
    // first we figure out which part of the current raster is desired
    val col1 = floor((xmin - geo.extent.xmin) / geo.cellwidth).toInt
    val row1 = floor((ymin - geo.extent.ymin) / geo.cellheight).toInt
    val col2 = ceil((xmax - geo.extent.xmin) / geo.cellwidth).toInt
    val row2 = ceil((ymax - geo.extent.ymin) / geo.cellheight).toInt

    // then translate back into our coordinates
    val xmin3 = geo.extent.xmin + col1 * geo.cellwidth
    val xmax3 = geo.extent.xmin + col2 * geo.cellwidth

    val ymin3 = geo.extent.ymin + row1 * geo.cellheight
    val ymax3 = geo.extent.ymin + row2 * geo.cellheight
    val cols3 = col2 - col1
    val rows3 = row2 - row1

    val extent = Extent(xmin3, ymin3, xmax3, ymax3)
    StepResult(RasterExtent(extent, geo.cellwidth, geo.cellheight, cols3, rows3))
  }
}
