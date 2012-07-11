package geotrellis.operation

import geotrellis.Extent
import geotrellis.RasterExtent
import geotrellis.process._

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class BuildRasterExtent(extent:Op[Extent], cols:Op[Int], rows:Op[Int])
  extends Op3 (extent,cols,rows) ({
  (e, cols, rows) => {
    val cw = (e.xmax - e.xmin) / cols
    val ch = (e.ymax - e.ymin) / rows
    Result(RasterExtent(e, cw, ch, cols, rows))
  }
})

object BuildRasterExtent {
  def apply(xmin:Double, ymin:Double, xmax:Double, ymax:Double, cols:Int, rows:Int):BuildRasterExtent = {
    BuildRasterExtent(Extent(xmin,ymin,xmax,ymax),cols,rows)
  }
}

object ParseRasterExtent {
  def apply(bbox:Op[String], colsOp:Op[String], rowsOp:Op[String]) = {
    BuildRasterExtent(ParseExtent(bbox), util.string.ParseInt(colsOp), util.string.ParseInt(rowsOp))
  }
}
