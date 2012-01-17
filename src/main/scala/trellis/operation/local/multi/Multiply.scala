package trellis.operation

import trellis._

case class Multiply(rs:Op[IntRaster]*) extends MultiLocal {
  override val identity = 1
  @inline final def handleCells2(a:Int, b:Int) = a * b
}
