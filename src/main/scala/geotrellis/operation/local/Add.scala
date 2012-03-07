package geotrellis.operation

import geotrellis._

/**
 * Add the values of each cell in each raster.
 */
case class Add(rs:Op[IntRaster]*) extends MultiLocal {
  final def ops = rs.toArray
  final def handle(a:Int, b:Int) = if (a == NODATA) b else if (b == NODATA) a else a + b
}

/**
 * Add the values of each cell in each raster.
 */
case class AddArray(op:Op[Array[IntRaster]]) extends MultiLocalArray {
  final def handle(a:Int, b:Int) = if (a == NODATA) b else if (b == NODATA) a else a + b
}
