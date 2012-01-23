package trellis.operation

import trellis._

/**
 * Add the values of each cell in each raster.
 */
case class Add(rs:Op[IntRaster]*) extends MultiLocal {
  final def ops = rs.toArray
  final def handle(a:Int, b:Int) = a + b
}

/**
 * Add the values of each cell in each raster.
 */
case class AddArray(op:Op[Array[IntRaster]]) extends MultiLocalArray {
  final def handle(a:Int, b:Int) = a + b
}
