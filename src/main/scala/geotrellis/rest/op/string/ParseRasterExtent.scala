package geotrellis.rest.op.string

import geotrellis._
import geotrellis.raster.op._

object ParseRasterExtent {
  def apply(bbox:Op[String], colsOp:Op[String], rowsOp:Op[String]) = {
    extent.GetRasterExtent(ParseExtent(bbox), ParseInt(colsOp), ParseInt(rowsOp))
  }
}
