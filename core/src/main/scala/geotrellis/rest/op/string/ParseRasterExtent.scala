package geotrellis.rest.op.string

import geotrellis._

object ParseRasterExtent {
  def apply(bbox:Op[String], colsOp:Op[String], rowsOp:Op[String]) = {
    (ParseExtent(bbox), ParseInt(colsOp), ParseInt(rowsOp)).map { (e,c,r) =>
      RasterExtent(e,c,r)
    }
  }
}
