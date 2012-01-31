package trellis.operation

import trellis._


/**
 * Multiply each cell of each raster.
 */
case class Multiply(rs:Op[IntRaster]*) extends MultiLocal {
  final def ops = rs.toArray
  final def handle(a:Int, b:Int) = a * b
}

/**
 * Multiply each cell of each raster in array.
 */
case class MultiplyArray(op:Op[Array[IntRaster]]) extends MultiLocalArray {
  final def handle(a:Int, b:Int) = a * b
}
