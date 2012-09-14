package geotrellis.rest.op.string

import geotrellis._
import geotrellis.raster.op.extent.GetRasterExtent

object ParseRasterExtent {
  def apply(bbox:Op[String], colsOp:Op[String], rowsOp:Op[String]) = {
    GetRasterExtent(ParseExtent(bbox), ParseInt(colsOp), ParseInt(rowsOp))
  }
}
