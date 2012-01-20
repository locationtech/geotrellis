package trellis.operation

import trellis._

case class Multiply(rs:Op[IntRaster]*) extends MultiLocal {
  final def ops = rs.toArray
  final def handle(a:Int, b:Int) = a * b
}

case class MultiplyArray(op:Op[Array[IntRaster]]) extends MultiLocalArray {
  final def handle(a:Int, b:Int) = a * b
}
