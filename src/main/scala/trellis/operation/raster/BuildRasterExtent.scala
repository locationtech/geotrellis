package trellis.operation

import trellis.Extent
import trellis.RasterExtent
import trellis.process.{Server, Results}

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class BuildRasterExtent(xmin:Double, ymin:Double,
                             xmax:Double, ymax:Double,
                             cols:Int, rows:Int) extends SimpleOp[RasterExtent] {
  val cellwidth  = (this.xmax - this.xmin) / this.cols
  val cellheight = (this.ymax - this.ymin) / this.rows

  def childOperations = List.empty[Op[_]]

  def _value(server:Server) = {
    val extent = Extent(xmin, ymin, xmax, ymax)
    RasterExtent(extent, cellwidth, cellheight, cols, rows)
  }
}

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class BuildRasterExtent2(extent:Op[Extent], cols:Op[Int], rows:Op[Int])
extends RasterExtentOperation {
  def childOperations = List(extent, cols, rows)

  val nextSteps:Steps = {
    case Results(List(e:Extent, c:Int, r:Int)) => step2(e, c, r)
  }

  def _run(server:Server, cb:Callback) = {
    runAsync(List(extent, cols, rows), server, cb)
  }

  def step2(e:Extent, cols:Int, rows:Int) = {
    val cw = (e.xmax - e.xmin) / cols
    val ch = (e.ymax - e.ymin) / rows
    Some(RasterExtent(e, cw, ch, cols, rows))
  }
}
